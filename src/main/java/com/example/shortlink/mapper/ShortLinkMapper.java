package com.example.shortlink.mapper;

import com.example.shortlink.model.ShortLink;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShortLinkMapper {
    int deleteByPrimaryKey(Long id);

    int insert(ShortLink row);

    int insertBatch(List<ShortLink> rows);

    int insertSelective(ShortLink record);

    ShortLink selectByPrimaryKey(Long id);

    List<ShortLink> selectAll();

    int updateByPrimaryKey(ShortLink row);

    ShortLink selectByCode(String code);

    int updateClickCount(ShortLink shortLink);
}