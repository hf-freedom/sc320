<template>
  <div class="devices">
    <h2 class="page-title">设备管理</h2>

    <el-card>
      <div slot="header" class="card-header">
        <span>灌溉设备列表</span>
        <div>
          <span style="margin-right: 20px;">最大并发数: {{ maxConcurrent }}</span>
          <el-button type="warning" size="mini" @click="showMaxConcurrentDialog">设置并发数</el-button>
          <el-button type="primary" @click="showAddDialog" style="margin-left: 10px;">
            <i class="el-icon-plus"></i> 新增设备
          </el-button>
        </div>
      </div>

      <el-table :data="devices" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="140"></el-table-column>
        <el-table-column prop="name" label="设备名称" width="140"></el-table-column>
        <el-table-column prop="capacity" label="容量(m³/h)" width="120"></el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusName(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fieldId" label="关联地块" width="140">
          <template slot-scope="scope">
            {{ scope.row.fieldId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="faultMessage" label="故障信息" width="160">
          <template slot-scope="scope">
            <span style="color: #F56C6C;">{{ scope.row.faultMessage || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="showEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="scope.row.status !== 'FAULT'" size="mini" type="danger" @click="reportFault(scope.row)">报故障</el-button>
            <el-button v-if="scope.row.status === 'FAULT'" size="mini" type="success" @click="repairDevice(scope.row)">修复</el-button>
            <el-button size="mini" type="danger" @click="deleteDevice(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-top: 20px;">
      <div slot="header" class="card-header">
        <span>📋 故障事件历史记录</span>
        <el-button type="text" size="mini" @click="loadFaultEvents">
          <i class="el-icon-refresh"></i> 刷新
        </el-button>
      </div>
      <el-empty v-if="faultEvents.length === 0" description="暂无故障事件记录"></el-empty>
      <el-timeline v-else>
        <el-timeline-item
          v-for="event in faultEvents"
          :key="event.id"
          :timestamp="formatTime(event.faultTime)"
          :type="event.resolved ? 'success' : 'danger'"
          :icon="event.resolved ? 'el-icon-circle-check' : 'el-icon-warning'">
          <el-card shadow="hover" class="event-card">
            <div class="event-header">
              <span class="event-title">
                <i class="el-icon-magic-stick" style="color: #F56C6C;"></i>
                设备故障事件
              </span>
              <el-tag :type="event.resolved ? 'success' : 'danger'" size="mini">
                {{ event.resolved ? '已处理' : '待处理' }}
              </el-tag>
            </div>
            <div class="event-body">
              <div class="event-row">
                <span class="event-label">故障设备:</span>
                <span class="event-value danger">{{ event.deviceName }}</span>
              </div>
              <div class="event-row">
                <span class="event-label">故障描述:</span>
                <span class="event-value">{{ event.faultMessage }}</span>
              </div>
              <div class="event-row" v-if="event.affectedFieldId">
                <span class="event-label">影响地块:</span>
                <span class="event-value warning">{{ event.affectedFieldId }}</span>
              </div>
              <div class="event-row">
                <span class="event-label">暂停计划:</span>
                <span class="event-value">{{ event.pausedPlansCount }} 个</span>
              </div>
              <div class="event-row" v-if="event.reallocatedPlansCount > 0">
                <span class="event-label">重新分配:</span>
                <span class="event-value success">
                  {{ event.reallocatedPlansCount }} 个计划 → {{ event.newDeviceName }}
                </span>
              </div>
              <div class="event-row" v-if="event.unallocatedPlans > 0">
                <span class="event-label">未分配:</span>
                <span class="event-value danger">{{ event.unallocatedPlans }} 个计划 (设备不足)</span>
              </div>
              <div class="event-row">
                <span class="event-label">处理结果:</span>
                <span class="event-value">{{ event.remarks }}</span>
              </div>
              <div class="event-row" v-if="event.resolvedTime">
                <span class="event-label">解决时间:</span>
                <span class="event-value success">{{ formatTime(event.resolvedTime) }}</span>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog title="设备信息" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="设备名称">
          <el-input v-model="form.name" placeholder="请输入设备名称"></el-input>
        </el-form-item>
        <el-form-item label="容量(m³/h)">
          <el-input-number v-model="form.capacity" :min="1" :step="0.5" controls-position="right"></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveDevice">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="设置最大并发数" :visible.sync="maxConcurrentVisible" width="400px">
      <el-form label-width="120px">
        <el-form-item label="最大并发数">
          <el-input-number v-model="newMaxConcurrent" :min="1" :max="10" controls-position="right"></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="maxConcurrentVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveMaxConcurrent">确 定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="上报故障" :visible.sync="faultDialogVisible" width="400px">
      <el-form label-width="100px">
        <el-form-item label="故障描述">
          <el-input type="textarea" v-model="faultMessage" rows="3" placeholder="请描述故障情况"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="faultDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmFault">确认上报</el-button>
      </div>
    </el-dialog>

    <el-dialog title="🔄 故障处理结果" :visible.sync="reallocateResultVisible" width="550px">
      <div v-if="lastReallocateResult">
        <el-result
          :icon="lastReallocateResult.reallocatedPlans > 0 ? 'success' : 'warning'"
          :title="lastReallocateResult.message"
          :sub-title="lastReallocateResult.event ? lastReallocateResult.event.remarks : ''">
          <div slot="extra" class="result-extra">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="故障设备">
                <span style="color: #F56C6C; font-weight: bold;">
                  {{ lastReallocateResult.event ? lastReallocateResult.event.deviceName : '-' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="影响地块">
                <span style="color: #E6A23C;">
                  {{ lastReallocateResult.affectedFieldId || '无' }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="已暂停计划">
                <el-tag type="warning">{{ lastReallocateResult.pausedPlans }} 个</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="已重新分配">
                <el-tag type="success">{{ lastReallocateResult.reallocatedPlans }} 个</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="未分配计划">
                <el-tag :type="lastReallocateResult.unallocatedPlans > 0 ? 'danger' : 'info'">
                  {{ lastReallocateResult.unallocatedPlans }} 个
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="新分配设备" v-if="lastReallocateResult.event && lastReallocateResult.event.newDeviceName">
                <span style="color: #67C23A; font-weight: bold;">
                  {{ lastReallocateResult.event.newDeviceName }}
                </span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-result>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="reallocateResultVisible = false">关 闭</el-button>
        <el-button type="primary" @click="goToPlans">查看灌溉计划</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
const statusNames = {
  RUNNING: '运行中', IDLE: '空闲', FAULT: '故障', MAINTENANCE: '维护中'
}

const statusTypes = {
  RUNNING: 'success', IDLE: 'info', FAULT: 'danger', MAINTENANCE: 'warning'
}

export default {
  name: 'Devices',
  data() {
    return {
      devices: [],
      faultEvents: [],
      maxConcurrent: 3,
      dialogVisible: false,
      isEdit: false,
      form: {
        id: '',
        name: '',
        capacity: 10.0
      },
      maxConcurrentVisible: false,
      newMaxConcurrent: 3,
      faultDialogVisible: false,
      faultMessage: '',
      currentDevice: null,
      reallocateResultVisible: false,
      lastReallocateResult: null
    }
  },
  mounted() {
    this.loadDevices()
    this.loadFaultEvents()
  },
  methods: {
    getStatusName(status) {
      return statusNames[status] || status
    },
    getStatusType(status) {
      return statusTypes[status] || 'info'
    },
    loadDevices() {
      this.$axios.get('/api/devices').then(res => {
        if (res.data.success) {
          this.devices = res.data.data
          this.maxConcurrent = res.data.maxConcurrent
        }
      })
    },
    showAddDialog() {
      this.isEdit = false
      this.form = { id: '', name: '', capacity: 10.0 }
      this.dialogVisible = true
    },
    showEditDialog(row) {
      this.isEdit = true
      this.form = { ...row }
      this.dialogVisible = true
    },
    saveDevice() {
      if (!this.form.name) {
        this.$message.warning('请输入设备名称')
        return
      }
      const request = this.isEdit
        ? this.$axios.put('/api/devices/' + this.form.id, this.form)
        : this.$axios.post('/api/devices', this.form)
      
      request.then(res => {
        if (res.data.success) {
          this.$message.success(this.isEdit ? '更新成功' : '添加成功')
          this.dialogVisible = false
          this.loadDevices()
        }
      })
    },
    deleteDevice(id) {
      this.$confirm('确定要删除该设备吗?', '提示', { type: 'warning' }).then(() => {
        this.$axios.delete('/api/devices/' + id).then(res => {
          if (res.data.success) {
            this.$message.success('删除成功')
            this.loadDevices()
          }
        })
      })
    },
    showMaxConcurrentDialog() {
      this.newMaxConcurrent = this.maxConcurrent
      this.maxConcurrentVisible = true
    },
    saveMaxConcurrent() {
      this.$axios.put('/api/devices/max-concurrent/' + this.newMaxConcurrent).then(res => {
        if (res.data.success) {
          this.$message.success(res.data.message)
          this.maxConcurrentVisible = false
          this.loadDevices()
        }
      })
    },
    reportFault(device) {
      this.currentDevice = device
      this.faultMessage = ''
      this.faultDialogVisible = true
    },
    formatTime(time) {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    },
    loadFaultEvents() {
      this.$axios.get('/api/devices/fault-events').then(res => {
        if (res.data.success) {
          this.faultEvents = res.data.data
        }
      })
    },
    confirmFault() {
      if (!this.faultMessage) {
        this.$message.warning('请输入故障描述')
        return
      }
      this.$axios.post('/api/devices/' + this.currentDevice.id + '/fault', {
        message: this.faultMessage
      }).then(res => {
        if (res.data.success) {
          this.faultDialogVisible = false
          this.lastReallocateResult = res.data.reallocateResult
          this.reallocateResultVisible = true
          this.loadDevices()
          this.loadFaultEvents()
        }
      })
    },
    repairDevice(device) {
      this.$confirm('确定设备已修复吗?', '提示', { type: 'warning' }).then(() => {
        this.$axios.post('/api/devices/' + device.id + '/repair').then(res => {
          if (res.data.success) {
            this.$message.success('设备已恢复正常')
            this.loadDevices()
            this.loadFaultEvents()
          }
        })
      })
    },
    goToPlans() {
      this.reallocateResultVisible = false
      this.$router.push('/plans')
    }
  }
}
</script>

<style scoped>
.devices {
  padding: 0;
}

.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #303133;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.event-card {
  border-radius: 8px;
  border: none;
  margin-bottom: 10px;
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.event-title {
  font-size: 15px;
  font-weight: bold;
  color: #303133;
}

.event-body {
  padding: 5px 0;
}

.event-row {
  display: flex;
  padding: 4px 0;
  font-size: 13px;
}

.event-label {
  color: #909399;
  width: 90px;
  flex-shrink: 0;
}

.event-value {
  color: #606266;
  flex: 1;
}

.event-value.danger {
  color: #F56C6C;
  font-weight: bold;
}

.event-value.warning {
  color: #E6A23C;
  font-weight: bold;
}

.event-value.success {
  color: #67C23A;
  font-weight: bold;
}

.result-extra {
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
}
</style>
