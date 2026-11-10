package com.example.petcommunity3.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.petcommunity3.dto.AppointmentCreateDTO;
import com.example.petcommunity3.dto.StatusUpdateDTO;
import com.example.petcommunity3.entity.Appointment;
import com.example.petcommunity3.entity.Pet;
import com.example.petcommunity3.entity.ServiceItem;
import com.example.petcommunity3.entity.Store;
import com.example.petcommunity3.entity.User;
import com.example.petcommunity3.mapper.AppointmentMapper;
import com.example.petcommunity3.mapper.PetMapper;
import com.example.petcommunity3.mapper.ServiceItemMapper;
import com.example.petcommunity3.mapper.StoreMapper;
import com.example.petcommunity3.mapper.UserMapper;
import com.example.petcommunity3.service.AppointmentService;
import com.example.petcommunity3.service.AppointmentSlotLockService;
import com.example.petcommunity3.service.MerchantPermissionService;
import com.example.petcommunity3.service.NotificationService;
import com.example.petcommunity3.service.ServiceScheduleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {
    private static final int STATUS_CANCELLED = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_COMPLETED = 2;
    private static final int STATUS_PENDING_MERCHANT = 3;

    // AppointmentServiceImpl 负责服务预约模块：创建预约、查询预约、取消预约、完成预约和更新预约状态。
    private final AppointmentMapper appointmentMapper;
    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final StoreMapper storeMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final NotificationService notificationService;
    // 排班服务负责锁定和释放时间槽，防止同一时间被重复预约。
    private final ServiceScheduleService serviceScheduleService;
    private final AppointmentSlotLockService appointmentSlotLockService;
    private final MerchantPermissionService merchantPermissionService;

    public AppointmentServiceImpl(AppointmentMapper appointmentMapper,
                                  UserMapper userMapper,
                                  PetMapper petMapper,
                                  StoreMapper storeMapper,
                                  ServiceItemMapper serviceItemMapper,
                                  NotificationService notificationService,
                                  ServiceScheduleService serviceScheduleService,
                                  AppointmentSlotLockService appointmentSlotLockService,
                                  MerchantPermissionService merchantPermissionService) {
        // 构造方法注入预约相关依赖，ServiceImpl 负责把多个表的操作串成完整业务流程。
        this.appointmentMapper = appointmentMapper;
        this.userMapper = userMapper;
        this.petMapper = petMapper;
        this.storeMapper = storeMapper;
        this.serviceItemMapper = serviceItemMapper;
        this.notificationService = notificationService;
        this.serviceScheduleService = serviceScheduleService;
        this.appointmentSlotLockService = appointmentSlotLockService;
        this.merchantPermissionService = merchantPermissionService;
    }

    @Override
    @Transactional
    public void create(Long operatorId, AppointmentCreateDTO dto) {
        // 用户提交时只创建待商户确认的申请，不占用排班名额。
        // 第一步检查请求字段是否齐全。
        validateCreateDTO(dto);
        requireSelfOrAdmin(operatorId, dto.getUserId());
        // 第二步检查用户、宠物、门店、服务项目是否真实可用。
        validateUserPetStoreAndService(dto);
        serviceScheduleService.validateSlot(
                dto.getServiceScheduleId(),
                dto.getStoreId(),
                dto.getServiceItemId(),
                dto.getAppointmentTime()
        );
        createPendingAppointment(dto);
    }

    @Override
    public List<Appointment> listByUserId(Long operatorId, Long userId) {
        // 用户端查询自己的预约记录。
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        requireSelfOrAdmin(operatorId, userId);

        return appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getUserId, userId)
                        .orderByDesc(Appointment::getCreateTime)
        );
    }

    @Override
    public List<Appointment> listByStoreId(Long operatorId, Long storeId) {
        // 商户端按门店查看预约记录。
        if (storeId == null) {
            throw new RuntimeException("门店ID不能为空");
        }
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, storeId);

        return appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getStoreId, storeId)
                        .orderByDesc(Appointment::getCreateTime)
        );
    }

    @Override
    @Transactional
    public void cancel(Long operatorId, Long id, Long userId) {
        // 用户取消预约：只能取消自己的、还处于已预约状态的记录。
        if (id == null) {
            throw new RuntimeException("预约ID不能为空");
        }
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        requireSelfOrAdmin(operatorId, userId);

        Appointment appointment = appointmentMapper.selectOne(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getId, id)
                        .eq(Appointment::getUserId, userId)
        );
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        Integer oldStatus = appointment.getStatus();
        if (!Integer.valueOf(STATUS_APPROVED).equals(oldStatus)
                && !Integer.valueOf(STATUS_PENDING_MERCHANT).equals(oldStatus)) {
            throw new RuntimeException("该预约无法取消");
        }

        appointment.setStatus(STATUS_CANCELLED);
        appointmentMapper.updateById(appointment);
        if (Integer.valueOf(STATUS_APPROVED).equals(oldStatus)) {
            // 只有已经通过的预约占用过名额，取消时才需要释放。
            serviceScheduleService.releaseSlot(appointment.getServiceScheduleId());
        }
        sendAppointmentNotification(appointment, userId,
                Integer.valueOf(STATUS_PENDING_MERCHANT).equals(oldStatus) ? "预约申请已取消" : "预约已取消");
    }

    @Override
    @Transactional
    public void finish(Long operatorId, Long id) {
        // 完成预约：通常由商户或后台操作，把状态从已预约改为已完成。
        if (id == null) {
            throw new RuntimeException("预约ID不能为空");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, appointment.getStoreId());
        if (!Integer.valueOf(STATUS_APPROVED).equals(appointment.getStatus())) {
            throw new RuntimeException("该预约无法完成");
        }

        appointment.setStatus(STATUS_COMPLETED);
        appointmentMapper.updateById(appointment);
        // 预约创建成功后，给用户生成一条预约通知。
        sendAppointmentNotification(appointment, operatorId, "预约已完成");
    }

    @Override
    @Transactional
    public void updateStatus(Long operatorId, Long id, StatusUpdateDTO dto) {
        // 后台通用状态修改：除了改状态，还要根据状态变化处理时间槽。
        if (id == null) {
            throw new RuntimeException("预约ID不能为空");
        }
        if (dto == null || dto.getStatus() == null) {
            throw new RuntimeException("预约状态不能为空");
        }
        if (!isValidStatus(dto.getStatus())) {
            throw new RuntimeException("预约状态无效");
        }

        Appointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw new RuntimeException("预约不存在");
        }
        merchantPermissionService.requireStoreOwnerOrAdmin(operatorId, appointment.getStoreId());

        Integer oldStatus = appointment.getStatus();
        Integer newStatus = dto.getStatus();
        if (Integer.valueOf(STATUS_PENDING_MERCHANT).equals(oldStatus)
                && Integer.valueOf(STATUS_APPROVED).equals(newStatus)) {
            appointmentSlotLockService.executeWithLock(buildSlotLockKey(appointment), () -> {
                serviceScheduleService.lockSlot(
                        appointment.getServiceScheduleId(),
                        appointment.getStoreId(),
                        appointment.getServiceItemId(),
                        appointment.getAppointmentTime()
                );
                appointment.setStatus(STATUS_APPROVED);
                appointmentMapper.updateById(appointment);
                return null;
            });
        } else if (Integer.valueOf(STATUS_PENDING_MERCHANT).equals(oldStatus)
                && Integer.valueOf(STATUS_CANCELLED).equals(newStatus)) {
            appointment.setStatus(STATUS_CANCELLED);
            appointmentMapper.updateById(appointment);
        } else if (Integer.valueOf(STATUS_APPROVED).equals(oldStatus)
                && Integer.valueOf(STATUS_CANCELLED).equals(newStatus)) {
            appointment.setStatus(STATUS_CANCELLED);
            appointmentMapper.updateById(appointment);
            serviceScheduleService.releaseSlot(appointment.getServiceScheduleId());
        } else if (Integer.valueOf(STATUS_APPROVED).equals(oldStatus)
                && Integer.valueOf(STATUS_COMPLETED).equals(newStatus)) {
            appointment.setStatus(STATUS_COMPLETED);
            appointmentMapper.updateById(appointment);
        } else {
            throw new RuntimeException("预约状态不允许这样变更");
        }

        String content = "预约状态已变更为：" + appointmentStatusName(newStatus);
        if (StringUtils.hasText(dto.getRemark())) {
            content = content + "，备注：" + dto.getRemark();
        }
        sendAppointmentNotification(appointment, operatorId, content);
    }

    private User requireSelfOrAdmin(Long operatorId, Long targetUserId) {
        User operator = merchantPermissionService.requireActiveUser(operatorId);
        if (!"ADMIN".equalsIgnoreCase(operator.getRole()) && !operatorId.equals(targetUserId)) {
            throw new RuntimeException("无权查看其他用户预约");
        }
        return operator;
    }

    private void createPendingAppointment(AppointmentCreateDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setUserId(dto.getUserId());
        appointment.setPetId(dto.getPetId());
        appointment.setStoreId(dto.getStoreId());
        appointment.setServiceItemId(dto.getServiceItemId());
        appointment.setServiceScheduleId(dto.getServiceScheduleId());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setRemark(dto.getRemark());
        appointment.setStatus(STATUS_PENDING_MERCHANT);
        appointmentMapper.insert(appointment);
        sendAppointmentNotification(appointment, null, "预约申请已提交，等待商户确认");
    }

    private String buildSlotLockKey(Appointment appointment) {
        if (appointment.getServiceScheduleId() != null) {
            return "schedule:" + appointment.getServiceScheduleId();
        }
        return "manual:" + appointment.getStoreId() + ":" + appointment.getServiceItemId() + ":" + appointment.getAppointmentTime();
    }

    private void validateCreateDTO(AppointmentCreateDTO dto) {
        // 创建预约的基础字段校验：这些字段缺任何一个都无法形成预约单。
        if (dto.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (dto.getPetId() == null) {
            throw new RuntimeException("宠物ID不能为空");
        }
        if (dto.getStoreId() == null) {
            throw new RuntimeException("门店ID不能为空");
        }
        if (dto.getServiceItemId() == null) {
            throw new RuntimeException("服务项目ID不能为空");
        }
        if (dto.getAppointmentTime() == null) {
            throw new RuntimeException("预约时间不能为空");
        }
        if (dto.getServiceScheduleId() == null) {
            throw new RuntimeException("请选择服务排班");
        }
    }

    private void validateUserPetStoreAndService(AppointmentCreateDTO dto) {
        // 跨表校验：确认用户可用、宠物属于该用户、门店和服务项目可用。
        User user = userMapper.selectById(dto.getUserId());
        if (user == null || Integer.valueOf(0).equals(user.getStatus())) {
            throw new RuntimeException("用户不存在或已被禁用");
        }

        Pet pet = petMapper.selectById(dto.getPetId());
        if (pet == null || !dto.getUserId().equals(pet.getUserId())) {
            throw new RuntimeException("宠物不存在");
        }

        Store store = storeMapper.selectById(dto.getStoreId());
        if (store == null || !Integer.valueOf(1).equals(store.getStatus())) {
            throw new RuntimeException("门店不存在");
        }

        ServiceItem serviceItem = serviceItemMapper.selectById(dto.getServiceItemId());
        if (serviceItem == null || !Integer.valueOf(1).equals(serviceItem.getStatus())) {
            throw new RuntimeException("服务项目不存在");
        }

        if (!dto.getStoreId().equals(serviceItem.getStoreId())) {
            throw new RuntimeException("服务项目不属于该门店");
        }
    }

    private void sendAppointmentNotification(Appointment appointment, Long fromUserId, String content) {
        // 预约状态变化后统一从这里创建通知，避免每个方法重复写通知代码。
        if (appointment.getUserId() == null) {
            return;
        }
        notificationService.create(
                appointment.getUserId(),
                fromUserId,
                "APPOINTMENT_STATUS",
                content,
                "APPOINTMENT",
                appointment.getId()
        );
    }

    private boolean isValidStatus(Integer status) {
        // 商户可执行的目标状态：0驳回/取消，1通过，2完成。
        return Integer.valueOf(STATUS_CANCELLED).equals(status)
                || Integer.valueOf(STATUS_APPROVED).equals(status)
                || Integer.valueOf(STATUS_COMPLETED).equals(status);
    }

    private String appointmentStatusName(Integer status) {
        // 把数字状态转换成通知里能读懂的文字。
        if (Integer.valueOf(STATUS_CANCELLED).equals(status)) {
            return "已取消";
        }
        if (Integer.valueOf(STATUS_APPROVED).equals(status)) {
            return "已预约";
        }
        if (Integer.valueOf(STATUS_COMPLETED).equals(status)) {
            return "已完成";
        }
        if (Integer.valueOf(STATUS_PENDING_MERCHANT).equals(status)) {
            return "待商户确认";
        }
        return "未知";
    }
}
