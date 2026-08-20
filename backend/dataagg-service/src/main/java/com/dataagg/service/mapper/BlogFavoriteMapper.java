package com.dataagg.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataagg.common.entity.BlogFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BlogFavoriteMapper extends BaseMapper<BlogFavorite> {

    @Select("<script>SELECT blog_id FROM blog_favorite WHERE user_id = #{userId}</script>")
    List<Long> selectFavoritedBlogIds(@Param("userId") Long userId);

    @Select("<script>SELECT blog_id, COUNT(*) as cnt FROM blog_favorite WHERE blog_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY blog_id</script>")
    List<Map<String, Object>> countGroupByBlogIds(@Param("ids") List<Long> blogIds);
}
