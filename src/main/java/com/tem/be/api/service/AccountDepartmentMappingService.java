package com.tem.be.api.service;

import com.tem.be.api.dto.AccountDepartmentMappingDTO;
import com.tem.be.api.model.AccountDepartmentMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AccountDepartmentMappingService {

    List<AccountDepartmentMapping> getMappingsByCarrier(String carrier);

    AccountDepartmentMapping getMappingById(Long id);

    AccountDepartmentMapping createMapping(AccountDepartmentMappingDTO mappingDTO);

    AccountDepartmentMapping updateMapping(Long id, AccountDepartmentMappingDTO mappingDTO);

    void deleteMapping(Long id);

    List<AccountDepartmentMapping> uploadMappings(MultipartFile file, String uploadedBy, String carrier) throws IOException;

    Map<String, String> getDepartmentMapping(String carrier);
}
