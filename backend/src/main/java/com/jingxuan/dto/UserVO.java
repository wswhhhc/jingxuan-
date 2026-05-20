package com.jingxuan.dto;

import com.jingxuan.entity.SysUser;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVO extends SysUser {

    private String roleName;
    private String className;

    public static UserVO from(SysUser user, String roleName, String className) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setDeleted(user.getDeleted());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRoleId(user.getRoleId());
        vo.setClassId(user.getClassId());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setFirstLogin(user.getFirstLogin());
        vo.setRoleName(roleName);
        vo.setClassName(className);
        return vo;
    }
}
