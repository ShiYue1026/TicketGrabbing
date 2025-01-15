package com.damai.service;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baidu.fsg.uid.UidGenerator;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.damai.dto.NotifyDto;
import com.damai.dto.PayDto;
import com.damai.dto.RefundDto;
import com.damai.dto.TradeCheckDto;
import com.damai.entity.PayBill;
import com.damai.entity.RefundBill;
import com.damai.enums.BaseCode;
import com.damai.enums.OrderStatus;
import com.damai.enums.PayBillStatus;
import com.damai.exception.DaMaiFrameException;
import com.damai.mapper.PayBillMapper;
import com.damai.mapper.RefundBillMapper;
import com.damai.pay.*;
import com.damai.servicelock.annotation.ServiceLock;
import com.damai.util.DateUtils;
import com.damai.vo.NotifyVo;
import com.damai.vo.TradeCheckVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

import static com.damai.constant.Constant.ALIPAY_NOTIFY_FAILURE_RESULT;
import static com.damai.constant.Constant.ALIPAY_NOTIFY_SUCCESS_RESULT;
import static com.damai.core.DistributedLockConstants.COMMON_PAY;
import static com.damai.core.DistributedLockConstants.TRADE_CHECK;

@Slf4j
@Service
public class PayService {

    @Autowired
    private PayBillMapper payBillMapper;

    @Autowired
    private PayStrategyContext payStrategyContext;

    @Autowired
    private UidGenerator uidGenerator;

    @Autowired
    private RefundBillMapper refundBillMapper;

    /**
     * 通用支付，用订单号加锁防止多次支付成功，不依赖第三方支付的幂等性
     * */
    @ServiceLock(name = COMMON_PAY, keys = {"#payDto.orderNumber"})
    @Transactional(rollbackFor = Exception.class)
    public String commonPay(PayDto payDto) {
        LambdaQueryWrapper<PayBill> queryWrapper = Wrappers.lambdaQuery(PayBill.class)
                .eq(PayBill::getOutOrderNo, payDto.getOrderNumber());
        PayBill payBill = payBillMapper.selectOne(queryWrapper);
        if(Objects.nonNull(payBill) && !Objects.equals(payBill.getPayBillStatus(), PayBillStatus.NO_PAY)){
            throw new DaMaiFrameException(BaseCode.PAY_BILL_IS_NOT_NO_PAY);
        }
        PayStrategyHandler payStrategyHandler = payStrategyContext.get(payDto.getChannel());
        PayResult pay = payStrategyHandler.pay(
                String.valueOf(payDto.getOrderNumber()),
                payDto.getPrice(),
                payDto.getSubject(),
                payDto.getNotifyUrl(),
                payDto.getReturnUrl()
        );

        if(pay.isSuccess()){
            payBill = new PayBill();
            payBill.setId(uidGenerator.getUid());
            payBill.setOutOrderNo(String.valueOf(payDto.getOrderNumber()));
            payBill.setPayChannel(payDto.getChannel());
            payBill.setPayScene("生产");
            payBill.setSubject(payDto.getSubject());
            payBill.setPayAmount(payDto.getPrice());
            payBill.setPayBillType(payDto.getPayBillType());
            payBill.setPayBillStatus(PayBillStatus.NO_PAY.getCode());
            payBill.setPayTime(DateUtils.now());
            payBillMapper.insert(payBill);
        }

        return pay.getBody();
    }

    @Transactional(rollbackFor = Exception.class)
    public NotifyVo notify(NotifyDto notifyDto){
        NotifyVo notifyVo = new NotifyVo();
        log.info("回调通知参数 ===> {}", JSON.toJSONString(notifyDto));
        Map<String, String> params = notifyDto.getParams();

        PayStrategyHandler payStrategyHandler = payStrategyContext.get(notifyDto.getChannel());
        // 验签
        boolean signVerifyResult = payStrategyHandler.signVerify(params);
        if(!signVerifyResult){
            notifyVo.setPayResult(ALIPAY_NOTIFY_FAILURE_RESULT);
            return notifyVo;
        }
        LambdaQueryWrapper<PayBill> payBillLambdaQueryWrapper = Wrappers.lambdaQuery(PayBill.class)
                .eq(PayBill::getOutOrderNo, params.get("out_trade_no"));
        PayBill payBill = payBillMapper.selectOne(payBillLambdaQueryWrapper);
        if(Objects.isNull(payBill)){
            log.error("账单为空 notifyDto: {}", JSON.toJSONString(notifyDto));
            notifyVo.setPayResult(ALIPAY_NOTIFY_FAILURE_RESULT);
            return notifyVo;
        }
        // 如果账单状态是 已支付/已取消/已退单 说明已经处理过了，直接返回
        if(Objects.equals(payBill.getPayBillStatus(), PayBillStatus.PAY.getCode())){
            log.info("账单已支付 notifyDto : {}", JSON.toJSONString(notifyDto));
            notifyVo.setOutTradeNo(payBill.getOutOrderNo());
            notifyVo.setPayResult(ALIPAY_NOTIFY_SUCCESS_RESULT);
            return notifyVo;
        }
        if(Objects.equals(payBill.getPayBillStatus(), PayBillStatus.CANCEL.getCode())){
            log.info("账单已取消 notifyDto : {}", JSON.toJSONString(notifyDto));
            notifyVo.setOutTradeNo(payBill.getOutOrderNo());
            notifyVo.setPayResult(ALIPAY_NOTIFY_SUCCESS_RESULT);
            return notifyVo;
        }
        if(Objects.equals(payBill.getPayBillStatus(), PayBillStatus.REFUND.getCode())){
            log.info("账单已退单 notifyDto : {}", JSON.toJSONString(notifyDto));
            notifyVo.setOutTradeNo(payBill.getOutOrderNo());
            notifyVo.setPayResult(ALIPAY_NOTIFY_SUCCESS_RESULT);
            return notifyVo;
        }

        // 验证数据
        boolean dataVerify = payStrategyHandler.dataVerify(notifyDto.getParams(), payBill);
        if(!dataVerify){
            notifyVo.setOutTradeNo(payBill.getOutOrderNo());
            notifyVo.setPayResult(ALIPAY_NOTIFY_FAILURE_RESULT);
            return notifyVo;
        }

        PayBill updatePayBill = new PayBill();
        updatePayBill.setPayBillStatus(PayBillStatus.PAY.getCode());  // 更新账单状态为已支付
        LambdaUpdateWrapper<PayBill> payBillLambdaUpdateWrapper = Wrappers.lambdaUpdate(PayBill.class)
                .eq(PayBill::getOutOrderNo, params.get("out_trade_no"));
        payBillMapper.update(updatePayBill, payBillLambdaUpdateWrapper);
        notifyVo.setOutTradeNo(payBill.getOutOrderNo());
        notifyVo.setPayResult(ALIPAY_NOTIFY_SUCCESS_RESULT);
        return notifyVo;
    }

    @Transactional(rollbackFor = Exception.class)
    public String refund(RefundDto refundDto) {
        LambdaQueryWrapper<PayBill> payBillLambdaQueryWrapper = Wrappers.lambdaQuery(PayBill.class)
                .eq(PayBill::getOutOrderNo, refundDto.getOrderNumber());
        PayBill payBill = payBillMapper.selectOne(payBillLambdaQueryWrapper);
        if(Objects.isNull(payBill)){
            throw new DaMaiFrameException(BaseCode.PAY_BILL_NOT_EXIST);
        }
        if(!Objects.equals(payBill.getPayBillStatus(), PayBillStatus.PAY.getCode())){
            throw new DaMaiFrameException(BaseCode.PAY_BILL_IS_NOT_PAY_STATUS);
        }
        if(refundDto.getAmount().compareTo(payBill.getPayAmount()) > 0){
            throw new DaMaiFrameException(BaseCode.REFUND_AMOUNT_GREATER_THAN_PAY_AMOUNT);
        }

        PayStrategyHandler payStrategyHandler = payStrategyContext.get(refundDto.getChannel());
        RefundResult refundResult = payStrategyHandler.refund(
                refundDto.getOrderNumber(),
                refundDto.getAmount(),
                refundDto.getReason());

        if(refundResult.isSuccess()){
            RefundBill refundBill = new RefundBill();
            refundBill.setId(uidGenerator.getUid());
            refundBill.setOutOrderNo(payBill.getOutOrderNo());
            refundBill.setPayBillId(payBill.getId());
            refundBill.setRefundAmount(refundDto.getAmount());
            refundBill.setRefundStatus(1);
            refundBill.setRefundTime(DateUtils.now());
            refundBill.setReason(refundDto.getReason());
            refundBillMapper.insert(refundBill);

            // 更新账单状态
            PayBill updatePayBill = new PayBill();
            updatePayBill.setPayBillStatus(PayBillStatus.REFUND.getCode());
            updatePayBill.setEditTime(DateUtils.now());
            LambdaUpdateWrapper<PayBill> payBillLambdaUpdateWrapper = Wrappers.lambdaUpdate(PayBill.class)
                            .eq(PayBill::getId, payBill.getId());
            payBillMapper.update(updatePayBill, payBillLambdaUpdateWrapper);
            return refundBill.getOutOrderNo();
        } else{
            throw new DaMaiFrameException(refundResult.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @ServiceLock(name = TRADE_CHECK, keys = {"#tradeCheckDto.outTradeNo"})
    public TradeCheckVo tradeCheck(TradeCheckDto tradeCheckDto) {
        TradeCheckVo tradeCheckVo = new TradeCheckVo();
        PayStrategyHandler payStrategyHandler = payStrategyContext.get(tradeCheckDto.getChannel());
        TradeResult tradeResult = payStrategyHandler.queryTrade(tradeCheckDto.getOutTradeNo());
        BeanUtil.copyProperties(tradeResult, tradeCheckVo);
        if(!tradeResult.isSuccess()){
            return tradeCheckVo;
        }
        BigDecimal totalAmount = tradeResult.getTotalAmount();
        String outTradeNo = tradeResult.getOutTradeNo();
        Integer payBillStatus = tradeResult.getPayBillStatus();
        LambdaQueryWrapper<PayBill> payBillLambdaQueryWrapper = Wrappers.lambdaQuery(PayBill.class)
                .eq(PayBill::getOutOrderNo, outTradeNo);
        PayBill payBill = payBillMapper.selectOne(payBillLambdaQueryWrapper);
        if(Objects.isNull(payBill)){
            log.error("账单为空 tradeCheckDto : {}",JSON.toJSONString(tradeCheckDto));
            return tradeCheckVo;
        }
        if(payBill.getPayAmount().compareTo(totalAmount) != 0){
            log.error("支付渠道 和库中账单支付金额不一致 支付渠道支付金额 : {}, 库中账单支付金额 : {}, tradeCheckDto : {}",
                    totalAmount,payBill.getPayAmount(),JSON.toJSONString(tradeCheckDto));
            return tradeCheckVo;
        }
        if(!Objects.equals(payBill.getPayBillStatus(), payBillStatus)) {
            log.warn("支付渠道和库中账单交易状态不一致 支付渠道payBillStatus : {}, 库中payBillStatus : {}, tradeCheckDto : {}",
                    payBillStatus,payBill.getPayBillStatus(),JSON.toJSONString(tradeCheckDto));
            // 更新账单状态
            PayBill updatePayBill = new PayBill();
            updatePayBill.setId(payBill.getId());
            updatePayBill.setPayBillStatus(payBillStatus);
            LambdaUpdateWrapper<PayBill> payBillLambdaUpdateWrapper = Wrappers.lambdaUpdate(PayBill.class)
                    .eq(PayBill::getOutOrderNo, outTradeNo);
            payBillMapper.update(updatePayBill, payBillLambdaUpdateWrapper);
            return tradeCheckVo;
        }
        return tradeCheckVo;
    }
}
