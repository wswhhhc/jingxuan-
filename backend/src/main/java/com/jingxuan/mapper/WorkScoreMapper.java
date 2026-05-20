package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.WorkScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WorkScoreMapper extends BaseMapper<WorkScore> {

    @Select("SELECT * FROM work_score WHERE work_id = #{workId} AND deleted = 0")
    List<WorkScore> selectByWorkId(@Param("workId") Long workId);

    @Select("SELECT * FROM work_score WHERE work_id = #{workId} AND teacher_id = #{teacherId} AND deleted = 0")
    WorkScore selectByWorkAndTeacher(@Param("workId") Long workId, @Param("teacherId") Long teacherId);
}
