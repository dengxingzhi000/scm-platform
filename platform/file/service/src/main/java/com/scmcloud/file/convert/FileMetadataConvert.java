package com.scmcloud.file.convert;

import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMetadataConvert {
    
    FileMetadataDTO toDTO(FileMetadata entity);
    
    @Mapping(target = "deleted", ignore = true)
    FileMetadata toEntity(FileMetadataDTO dto);
}
