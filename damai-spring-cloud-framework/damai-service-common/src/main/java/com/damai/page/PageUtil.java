package com.damai.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.damai.dto.BasePageDto;
import com.github.pagehelper.PageInfo;

import java.util.function.Function;
import java.util.stream.Collectors;

public class PageUtil {

    /**
     * 组装分页参数
     **/
    public static <T> IPage<T> getPageParams(BasePageDto basePageDto) {
        return getPageParams(basePageDto.getPageNumber(), basePageDto.getPageSize());
    }

    /**
     * 组装分页参数
     **/
    public static <T> IPage<T> getPageParams(int pageNumber, int pageSize) {
        return new Page<>(pageNumber, pageSize);
    }

    /**
     * 转换分页对象
     * @param pageInfo PageInfo类型的分页对象
     * @param function 分页中的数据加工接口：输入参数必须是OLD或OLD的父类 返回参数必须是NEW或NEW的子类
     * @param <OLD> 旧数据实体类型
     * @param <NEW> 新数据实体类型
     * */
    public static <OLD, NEW> PageVo<NEW> convertPage(PageInfo<OLD> pageInfo, Function<? super OLD, ? extends NEW> function) {
        return new PageVo<>(pageInfo.getPageNum(),
                            pageInfo.getPageSize(),
                            pageInfo.getTotal(),
                            pageInfo.getList().stream().map(function).collect(Collectors.toList()));
    }

    /**
     * 转换分页对象
     * @param iPage IPage类型的分页对象
     * @param function 分页中的数据加工接口：输入参数必须是OLD或OLD的父类 返回参数必须是NEW或NEW的子类
     * @param <OLD> 旧数据实体类型
     * @param <NEW> 新数据实体类型
     * */
    public static <OLD, NEW> PageVo<NEW> convertPage(IPage<OLD> iPage, Function<? super OLD, ? extends NEW> function) {
        return new PageVo<>(iPage.getCurrent(),
                            iPage.getSize(),
                            iPage.getTotal(),
                            iPage.getRecords().stream().map(function).collect(Collectors.toList()));
    }
}
