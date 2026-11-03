package com.cognora.platform.file.convert;

import com.cognora.platform.file.api.dto.FileMetadataDTO;
import com.cognora.platform.file.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMetadataConvert {
    
    FileMetadataDTO toDTO(FileMetadata entity);
    
    @Mapping(target = "deleted", ignore = true)
    FileMetadata toEntity(FileMetadataDTO dto);
}
