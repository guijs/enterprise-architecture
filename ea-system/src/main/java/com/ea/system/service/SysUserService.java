package com.ea.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ea.common.domain.PageResult;
import com.ea.common.exception.BusinessException;
import com.ea.system.domain.dto.UserQueryRequest;
import com.ea.system.domain.dto.UserSaveRequest;
import com.ea.system.domain.entity.SysUser;
import com.ea.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    private final PasswordEncoder passwordEncoder;

    public SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username).last("LIMIT 1"));
    }

    public PageResult<SysUser> pageUsers(UserQueryRequest request) {
        Page<SysUser> page = page(
                new Page<>(request.safePageNum(), request.safePageSize()),
                new LambdaQueryWrapper<SysUser>()
                        .like(StringUtils.hasText(request.getUsername()), SysUser::getUsername, request.getUsername())
                        .like(StringUtils.hasText(request.getNickname()), SysUser::getNickname, request.getNickname())
                        .eq(request.getStatus() != null, SysUser::getStatus, request.getStatus())
                        .orderByDesc(SysUser::getCreateTime)
        );
        return PageResult.of(page);
    }

    public Long createUser(UserSaveRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("新增用户时密码不能为空");
        }
        if (getByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = toEntity(request);
        user.setId(null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(0);
        }
        if (user.getGender() == null) {
            user.setGender(0);
        }
        save(user);
        return user.getId();
    }

    public void updateUser(UserSaveRequest request) {
        if (request.getId() == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        SysUser exists = getById(request.getId());
        if (exists == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser sameName = getByUsername(request.getUsername());
        if (sameName != null && !sameName.getId().equals(request.getId())) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = toEntity(request);
        user.setPassword(null);
        updateById(user);
        if (StringUtils.hasText(request.getPassword())) {
            SysUser passwordUpdate = new SysUser();
            passwordUpdate.setId(request.getId());
            passwordUpdate.setPassword(passwordEncoder.encode(request.getPassword()));
            updateById(passwordUpdate);
        }
    }

    public void deleteUser(Long id) {
        if (id == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        if (id == 1L) {
            throw new BusinessException("系统管理员不可删除");
        }
        removeById(id);
    }

    private SysUser toEntity(UserSaveRequest request) {
        SysUser user = new SysUser();
        user.setId(request.getId());
        user.setUsername(request.getUsername());
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setStatus(request.getStatus());
        user.setDeptId(request.getDeptId());
        user.setRemark(request.getRemark());
        return user;
    }
}
