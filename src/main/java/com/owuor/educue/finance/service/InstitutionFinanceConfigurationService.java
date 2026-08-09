package com.owuor.educue.finance.service;

import com.owuor.educue.finance.dto.InstitutionFinanceConfigurationResponse;
import com.owuor.educue.finance.entity.InstitutionFinanceConfiguration;
import com.owuor.educue.finance.repository.InstitutionFinanceConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class InstitutionFinanceConfigurationService {
    private final InstitutionFinanceConfigurationRepository repository;
    @Value("${app.mpesa.c2b-short-code:}") private String configuredShortCode;
    @Transactional(readOnly=true) public InstitutionFinanceConfigurationResponse get(){
        var value=repository.findById(1L).orElse(null);
        return new InstitutionFinanceConfigurationResponse(value==null||blank(value.getPaybillShortCode())?configuredShortCode:value.getPaybillShortCode(),
                value==null?"Use the student's admission number as the account reference":value.getAccountReferenceInstructions(),
                value==null?null:value.getBankName(),value==null?null:value.getBankAccountName(),value==null?null:value.getBankAccountNumber(),value==null?null:value.getBankBranch());
    }
    @Transactional public InstitutionFinanceConfigurationResponse save(InstitutionFinanceConfigurationResponse request){
        var value=repository.findById(1L).orElseGet(InstitutionFinanceConfiguration::new);
        value.setPaybillShortCode(clean(request.paybillShortCode())); value.setAccountReferenceInstructions(clean(request.accountReferenceInstructions()));
        value.setBankName(clean(request.bankName())); value.setBankAccountName(clean(request.bankAccountName())); value.setBankAccountNumber(clean(request.bankAccountNumber())); value.setBankBranch(clean(request.bankBranch()));
        repository.save(value); return get();
    }
    private boolean blank(String v){return v==null||v.isBlank();} private String clean(String v){return blank(v)?null:v.trim();}
}
