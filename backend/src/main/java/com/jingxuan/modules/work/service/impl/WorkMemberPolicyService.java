package com.jingxuan.modules.work.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.SysUser;
import com.jingxuan.entity.WorkMember;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.mapper.WorkMemberMapper;
import com.jingxuan.modules.work.dto.WorkMemberDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
class WorkMemberPolicyService {

    private final SysUserMapper sysUserMapper;
    private final WorkMemberMapper workMemberMapper;

    void resolveMemberStudentIds(List<WorkMemberDTO> members) {
        if (CollectionUtil.isEmpty(members)) {
            return;
        }
        for (WorkMemberDTO member : members) {
            if (member.getStudentId() == null && StrUtil.isNotBlank(member.getStudentNo())) {
                SysUser user = sysUserMapper.findByUsername(member.getStudentNo());
                if (user != null) {
                    member.setStudentId(user.getId());
                }
            }
        }
    }

    void ensureMembersAvailableInBatch(List<WorkMemberDTO> members, Long batchId, Long currentWorkId) {
        if (CollectionUtil.isEmpty(members) || batchId == null) {
            return;
        }
        for (WorkMemberDTO member : members) {
            if (member.getStudentId() == null) {
                continue;
            }
            String workFilter = "SELECT id FROM work WHERE batch_id = " + batchId + " AND deleted = 0";
            if (currentWorkId != null) {
                workFilter = "SELECT id FROM work WHERE id != " + currentWorkId + " AND batch_id = " + batchId + " AND deleted = 0";
            }
            Long memberCount = workMemberMapper.selectCount(
                    Wrappers.<WorkMember>lambdaQuery()
                            .eq(WorkMember::getStudentId, member.getStudentId())
                            .inSql(WorkMember::getWorkId, workFilter));
            if (memberCount > 0) {
                throw new BusinessException("团队成员 " + member.getStudentName() + " 在当前批次中已有作品，每个学生只能参与一个作品");
            }
        }
    }
}
