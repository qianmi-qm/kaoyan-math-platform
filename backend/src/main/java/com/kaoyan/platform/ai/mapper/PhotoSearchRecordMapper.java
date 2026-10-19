package com.kaoyan.platform.ai.mapper;

import com.kaoyan.platform.ai.entity.PhotoSearchRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PhotoSearchRecordMapper {

    int insert(PhotoSearchRecord record);

    PhotoSearchRecord selectByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);

    List<PhotoSearchRecord> selectPage(@Param("userId") Long userId,
                                       @Param("favorite") Boolean favorite,
                                       @Param("offset") int offset,
                                       @Param("size") int size);

    long count(@Param("userId") Long userId, @Param("favorite") Boolean favorite);

    int updateFavorite(@Param("id") Long id, @Param("userId") Long userId,
                       @Param("favorite") boolean favorite);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
