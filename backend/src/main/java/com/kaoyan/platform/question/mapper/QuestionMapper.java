package com.kaoyan.platform.question.mapper;

import com.kaoyan.platform.question.entity.Question;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionMapper {

    /** 插入题目，主键回填到 question.id（XML 里配 useGeneratedKeys） */
    int insert(Question question);

    /** 插入题目-知识点关联 */
    int insertQuestionKp(@Param("questionId") Long questionId, @Param("kpId") Long kpId);

    /** 分页查询：kpId 为空查全部；按知识点查时需 join question_kp */
    List<Question> selectPage(@Param("kpId") Long kpId, @Param("offset") int offset, @Param("size") int size);

    /** 与 selectPage 同条件计数 */
    long countTotal(@Param("kpId") Long kpId);

    Question selectById(@Param("id") Long id);

    /** 批量查题（调用方保证 ids 非空） */
    List<Question> selectByIds(@Param("ids") List<Long> ids);

    /** 批量查题目-知识点关联（调用方保证 questionIds 非空） */
    List<QuestionKpRow> selectKpRowsByQuestionIds(@Param("questionIds") List<Long> questionIds);

    /** 导入幂等：按 content_hash 查重 */
    Question selectByContentHash(@Param("contentHash") String contentHash);

    List<Long> selectKpIds(@Param("questionId") Long questionId);

    /** 随机抽题（练习模块用）：按知识点抽 n 道 */
    List<Question> selectRandomByKp(@Param("kpId") Long kpId, @Param("userId") Long userId, @Param("n") int n);

    /** 错题重练：取该用户最近答错的题 */
    List<Question> selectWrongQuestions(@Param("userId") Long userId, @Param("n") int n);
}
