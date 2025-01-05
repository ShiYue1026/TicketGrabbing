package com.damai.service.composite;

import com.damai.dto.ProgramOrderCreateDto;
import com.damai.enums.CompositeCheckType;
import com.damai.initialize.impl.composite.AbstractComposite;

public abstract class AbstractProgramCheckHandler extends AbstractComposite<ProgramOrderCreateDto> {

    @Override
    protected String type() {
        return CompositeCheckType.PROGRAM_ORDER_CREATE_CHECK.getValue();
    }

}
