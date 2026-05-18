<template>
  <div class="plans">
    <h2 class="page-title">灌溉计划</h2>

    <el-card>
      <div slot="header" class="card-header">
        <span>灌溉计划列表</span>
        <el-button type="primary" @click="generatePlans">
          <i class="el-icon-set-up"></i> 智能生成计划
        </el-button>
      </div>

      <el-table :data="plans" border stripe style="width: 100%">
        <el-table-column type="expand">
          <template slot-scope="props">
            <div class="plan-detail">
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="detail-card">
                    <div class="detail-title">📊 用水计算依据</div>
                    <div class="detail-item">
                      <span class="label">生长期阈值:</span>
                      <span class="value">{{ parseReason(props.row.reason).threshold }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">当前湿度:</span>
                      <span class="value">{{ parseReason(props.row.reason).moisture }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">缺水缺口:</span>
                      <span class="value danger">{{ parseReason(props.row.reason).deficit }}%</span>
                    </div>
                    <div class="detail-item">
                      <span class="label">计划用水量:</span>
                      <span class="value primary">{{ props.row.waterAmount.toFixed(1) }} m³</span>
                    </div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="detail-card">
                    <div class="detail-title">🎯 优先级评分</div>
                    <div class="priority-score">
                      <div class="score-circle" :style="{ background: getPriorityGradient(props.row.priority) }">
                        <span>{{ props.row.priority }}</span>
                      </div>
                    </div>
                    <div class="priority-factors">
                      <div v-for="(factor, idx) in parseReason(props.row.reason).factors" :key="idx" class="factor-item">
                        <span class="factor-label">{{ factor.label }}</span>
                        <span class="factor-value" :class="factor.type">{{ factor.value }}</span>
                      </div>
                    </div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="detail-card">
                    <div class="detail-title">📝 策略调整说明</div>
                    <div class="strategy-notes">
                      <div v-for="(note, idx) in parseReason(props.row.reason).adjustments" :key="idx" class="note-item">
                        <i class="el-icon-right" style="color: #409EFF;"></i>
                        <span>{{ note }}</span>
                      </div>
                      <div v-if="parseReason(props.row.reason).adjustments.length === 0" class="no-adjustment">
                        按标准策略执行
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
              <el-row :gutter="20" style="margin-top: 20px;" v-if="parseReallocation(props.row.reason).isReallocated">
                <el-col :span="24">
                  <div class="reallocation-banner">
                    <div class="banner-icon">
                      <i class="el-icon-warning"></i>
                    </div>
                    <div class="banner-content">
                      <div class="banner-title">设备故障重新分配记录</div>
                      <div class="banner-detail">
                        <span class="old-device">原设备: {{ parseReallocation(props.row.reason).oldDevice }}</span>
                        <i class="el-icon-arrow-right" style="margin: 0 10px; color: #F56C6C;"></i>
                        <span class="new-device">新设备: {{ parseReallocation(props.row.reason).newDevice }}</span>
                      </div>
                      <div class="banner-time">
                        因设备故障，系统自动暂停该地块灌溉并重新分配可用设备
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="计划ID" width="200"></el-table-column>
        <el-table-column prop="fieldId" label="地块ID" width="120"></el-table-column>
        <el-table-column prop="deviceId" label="设备ID" width="120"></el-table-column>
        <el-table-column label="灌溉时间" width="280">
          <template slot-scope="scope">
            <div>开始: {{ formatTime(scope.row.startTime) }}</div>
            <div>结束: {{ formatTime(scope.row.endTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="用水量" width="140">
          <template slot-scope="scope">
            <div class="water-amount">
              <span class="amount-value">{{ scope.row.waterAmount.toFixed(1) }}</span>
              <span class="amount-unit">m³</span>
            </div>
            <div class="water-calc" v-if="parseReason(scope.row.reason).calcFormula">
              {{ parseReason(scope.row.reason).calcFormula }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100">
          <template slot-scope="scope">
            <el-tag :type="getPriorityType(scope.row.priority)" size="mini">
              {{ scope.row.priority }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="mini">
              {{ getStatusName(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="重新分配" width="130">
          <template slot-scope="scope">
            <el-tag v-if="parseReallocation(scope.row.reason).isReallocated" type="danger" size="mini">
              <i class="el-icon-refresh"></i> 已重分配
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="manualModified" label="人工修改" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.manualModified" type="warning" size="mini">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="primary"
              @click="showEditDialog(scope.row)"
              :disabled="scope.row.status === 'COMPLETED' || scope.row.status === 'CANCELLED'">
              编辑
            </el-button>
            <el-button
              size="mini"
              type="danger"
              @click="cancelPlan(scope.row.id)"
              :disabled="scope.row.status === 'COMPLETED' || scope.row.status === 'CANCELLED'">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog title="编辑灌溉计划" :visible.sync="dialogVisible" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%;">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%;">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="用水量(m³)">
          <el-input-number v-model="form.waterAmount" :min="0.1" :step="0.5" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="100" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="设备">
          <el-select v-model="form.deviceId" placeholder="选择设备">
            <el-option
              v-for="device in availableDevices"
              :key="device.id"
              :label="device.name + ' (' + device.status + ')'"
              :value="device.id">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="savePlan">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
const statusNames = {
  PENDING: '待执行', IN_PROGRESS: '执行中', COMPLETED: '已完成',
  PAUSED: '已暂停', CANCELLED: '已取消'
}

const statusTypes = {
  PENDING: 'warning', IN_PROGRESS: 'primary', COMPLETED: 'success',
  PAUSED: 'info', CANCELLED: 'danger'
}

const stageThresholds = {
  SEEDLING: 0.3, VEGETATIVE: 0.6, FLOWERING: 0.9, FRUITING: 0.8, RIPENING: 0.4
}

const stageNames = {
  SEEDLING: '苗期', VEGETATIVE: '营养生长期', FLOWERING: '开花期',
  FRUITING: '结果期', RIPENING: '成熟期'
}

export default {
  name: 'Plans',
  data() {
    return {
      plans: [],
      dialogVisible: false,
      availableDevices: [],
      form: {
        id: '',
        fieldId: '',
        deviceId: '',
        startTime: null,
        endTime: null,
        waterAmount: 0,
        priority: 50
      }
    }
  },
  mounted() {
    this.loadPlans()
    this.loadDevices()
  },
  methods: {
    formatTime(time) {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    },
    getStatusName(status) {
      return statusNames[status] || status
    },
    getStatusType(status) {
      return statusTypes[status] || 'info'
    },
    getPriorityType(priority) {
      if (priority >= 70) return 'danger'
      if (priority >= 40) return 'warning'
      return 'success'
    },
    getPriorityGradient(priority) {
      if (priority >= 70) return 'linear-gradient(135deg, #F56C6C, #f78989)'
      if (priority >= 40) return 'linear-gradient(135deg, #E6A23C, #f0c78a)'
      return 'linear-gradient(135deg, #67C23A, #95d475)'
    },
    parseReason(reason) {
      const result = {
        threshold: '0',
        moisture: '0',
        deficit: '0',
        factors: [],
        adjustments: [],
        calcFormula: ''
      }

      if (!reason) return result

      const moistureMatch = reason.match(/土壤湿度:([\d.]+)%/)
      if (moistureMatch) {
        result.moisture = moistureMatch[1]
      }

      const stageMatch = reason.match(/生长期:([^,]+)/)
      if (stageMatch) {
        const stageKey = Object.keys(stageNames).find(k => stageNames[k] === stageMatch[1])
        if (stageKey) {
          result.threshold = (stageThresholds[stageKey] * 100).toFixed(0)
          result.deficit = (stageThresholds[stageKey] * 100 - parseFloat(result.moisture)).toFixed(0)
        }
      }

      const tempMatch = reason.match(/气温:([\d.]+)℃/)
      if (tempMatch) {
        const temp = parseFloat(tempMatch[1])
        result.factors.push({
          label: '气温影响',
          value: temp + '℃',
          type: temp > 30 ? 'warning' : 'normal'
        })
        if (temp > 30) {
          result.adjustments.push(`高温天气(${temp}℃)，增加用水量`)
        }
      }

      const highTempMatch = reason.match(/连续高温(\d+)天/)
      if (highTempMatch) {
        result.factors.push({
          label: '连续高温',
          value: highTempMatch[1] + '天',
          type: 'danger'
        })
        result.adjustments.push(`连续高温${highTempMatch[1]}天，提升灌溉优先级`)
      }

      const cropMatch = reason.match(/作物类型:([^,]+)/)
      if (cropMatch) {
        result.factors.push({
          label: '作物类型',
          value: cropMatch[1],
          type: 'normal'
        })
      }

      if (stageMatch) {
        result.factors.push({
          label: '生长期',
          value: stageMatch[1],
          type: stageMatch[1].includes('开花') || stageMatch[1].includes('结果') ? 'warning' : 'normal'
        })
        if (stageMatch[1].includes('开花') || stageMatch[1].includes('结果')) {
          result.adjustments.push(`${stageMatch[1]}是关键生长期，增加灌溉频率`)
        }
      }

      const priorityMatch = reason.match(/优先级:(\d+)/)
      if (priorityMatch) {
        result.factors.push({
          label: '最终优先级',
          value: priorityMatch[1],
          type: parseInt(priorityMatch[1]) >= 70 ? 'danger' : parseInt(priorityMatch[1]) >= 40 ? 'warning' : 'success'
        })
      }

      if (parseFloat(result.deficit) > 0) {
        result.calcFormula = `缺水量: ${result.deficit}%`
      }

      if (reason.includes('重新分配')) {
        result.adjustments.push('设备故障重新分配资源')
      }

      return result
    },
    parseReallocation(reason) {
      const result = {
        isReallocated: false,
        oldDevice: '',
        newDevice: ''
      }

      if (!reason) return result

      const reallocMatch = reason.match(/设备故障重新分配: ([^→]+) → ([^\]]+)/)
      if (reallocMatch) {
        result.isReallocated = true
        result.oldDevice = reallocMatch[1].trim()
        result.newDevice = reallocMatch[2].trim()
      }

      return result
    },
    loadPlans() {
      this.$axios.get('/api/plans').then(res => {
        if (res.data.success) {
          this.plans = res.data.data.sort((a, b) => b.priority - a.priority)
        }
      })
    },
    loadDevices() {
      this.$axios.get('/api/devices').then(res => {
        if (res.data.success) {
          this.availableDevices = res.data.data
        }
      })
    },
    generatePlans() {
      this.$confirm('确定要生成新的灌溉计划吗? 这将根据当前的地块、天气和设备情况智能生成。', '提示', {
        type: 'warning'
      }).then(() => {
        this.$axios.post('/api/plans/generate').then(res => {
          if (res.data.success) {
            this.$message.success(`成功生成 ${res.data.count} 个灌溉计划`)
            this.loadPlans()
          } else {
            this.$message.error(res.data.message)
          }
        })
      })
    },
    showEditDialog(row) {
      this.form = {
        id: row.id,
        fieldId: row.fieldId,
        deviceId: row.deviceId,
        startTime: new Date(row.startTime),
        endTime: new Date(row.endTime),
        waterAmount: row.waterAmount,
        priority: row.priority
      }
      this.dialogVisible = true
    },
    savePlan() {
      if (this.form.startTime >= this.form.endTime) {
        this.$message.warning('结束时间必须大于开始时间')
        return
      }
      this.$axios.put('/api/plans/' + this.form.id, this.form).then(res => {
        if (res.data.success) {
          this.$message.success('计划更新成功，已通过冲突校验')
          this.dialogVisible = false
          this.loadPlans()
        } else {
          if (res.data.conflicts) {
            this.$message.error(res.data.conflicts.join('; '))
          } else {
            this.$message.error(res.data.message)
          }
        }
      })
    },
    cancelPlan(id) {
      this.$confirm('确定要取消该灌溉计划吗?', '提示', { type: 'warning' }).then(() => {
        this.$axios.post('/api/plans/' + id + '/cancel').then(res => {
          if (res.data.success) {
            this.$message.success('计划已取消')
            this.loadPlans()
          }
        })
      })
    }
  }
}
</script>

<style scoped>
.plans {
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

.plan-detail {
  padding: 10px 20px;
  background: #F5F7FA;
}

.detail-card {
  background: white;
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.detail-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #EBEEF5;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}

.detail-item .label {
  color: #909399;
}

.detail-item .value {
  font-weight: bold;
  color: #303133;
}

.detail-item .value.primary {
  color: #409EFF;
}

.detail-item .value.danger {
  color: #F56C6C;
}

.water-amount {
  display: flex;
  align-items: baseline;
}

.amount-value {
  font-size: 18px;
  font-weight: bold;
  color: #409EFF;
}

.amount-unit {
  font-size: 12px;
  color: #909399;
  margin-left: 4px;
}

.water-calc {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.priority-score {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.score-circle {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  font-weight: bold;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.priority-factors {
  border-top: 1px solid #EBEEF5;
  padding-top: 10px;
}

.factor-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 12px;
}

.factor-label {
  color: #909399;
}

.factor-value {
  font-weight: bold;
}

.factor-value.danger {
  color: #F56C6C;
}

.factor-value.warning {
  color: #E6A23C;
}

.factor-value.success {
  color: #67C23A;
}

.factor-value.normal {
  color: #606266;
}

.strategy-notes {
  min-height: 80px;
}

.note-item {
  display: flex;
  align-items: flex-start;
  padding: 4px 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}

.note-item i {
  margin-right: 6px;
  flex-shrink: 0;
}

.no-adjustment {
  text-align: center;
  color: #909399;
  font-size: 13px;
  padding: 20px 0;
}

.reallocation-banner {
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, #FEF0F0 0%, #FEE2E2 100%);
  border: 1px solid #FECACA;
  border-radius: 8px;
  padding: 15px 20px;
}

.banner-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: #FEE2E2;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 24px;
  color: #EF4444;
}

.banner-content {
  flex: 1;
}

.banner-title {
  font-size: 15px;
  font-weight: bold;
  color: #B91C1C;
  margin-bottom: 5px;
}

.banner-detail {
  font-size: 13px;
  color: #606266;
  margin-bottom: 3px;
}

.banner-detail .old-device {
  color: #EF4444;
  font-weight: bold;
}

.banner-detail .new-device {
  color: #10B981;
  font-weight: bold;
}

.banner-time {
  font-size: 12px;
  color: #909399;
}
</style>
