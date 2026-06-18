package com.scmcloud.file.convert;

import com.scmcloud.file.api.dto.FileMetadataDTO;
import com.scmcloud.file.entity.FileMetadata;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FileMetadataConvert {
    
    FileMetadataDTO toDTO(FileMetadata entity);
    
    FileMetadata toEntity(FileMetadataDTO dto);
}
