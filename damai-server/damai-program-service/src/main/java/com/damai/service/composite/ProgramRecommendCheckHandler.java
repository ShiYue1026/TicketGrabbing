package com.damai.service.composite;

import com.damai.dto.ProgramRecommendListDto;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.exception.DaMaiFrameException;
import com.damai.initialize.impl.composite.AbstractComposite;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ProgramRecommendCheckHandler extends AbstractComposite<ProgramRecommendListDto> {

    @Override
    protected void execute(ProgramRecommendListDto param) {
        if(Objects.isNull(param.getAreaId()) &&
           Objects.isNull(param.getProgramId()) &&
           Objects.isNull(param.getParentProgramCategoryId())) {
            throw new DaMaiFrameException(BaseCode.PARAMETERS_CANNOT_BE_EMPTY);
        }
    }

    @Override
    protected String type() {
        return CompositeCheckType.PROGRAM_RECOMMEND_CHECK.getValue();
    }

    @Override
    public Integer executeParentOrder() {
        return 0;
    }

    @Override
    public Integer executeTier() {
        return 1;
    }

    @Override
    public Integer executeOrder() {
        return 1;
    }
}
