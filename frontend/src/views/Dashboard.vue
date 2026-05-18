<template>
  <div class="dashboard">
    <h2 class="page-title">系统概览</h2>

    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409EFF;">
              <i class="el-icon-location-outline"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.totalFields || 0 }}</div>
              <div class="stat-label">地块总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67C23A;">
              <i class="el-icon-cpu"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.runningDevices || 0 }}/{{ overview.totalDevices || 0 }}</div>
              <div class="stat-label">运行设备</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #E6A23C;">
              <i class="el-icon-date"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ overview.inProgressPlans || 0 }}</div>
              <div class="stat-label">执行中计划</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-icon" style="background: #F56C6C;">
              <i class="el-icon-umbrella"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatWater(overview.remainingWater) }}</div>
              <div class="stat-label">剩余水量(m³)</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="info-card">
          <div slot="header">
            <span>🌤️ 当前天气</span>
            <el-button type="primary" size="mini" @click="simulateWeather">模拟天气</el-button>
          </div>
          <div class="weather-info" v-if="weather">
            <div class="weather-temp">{{ weather.temperature }}°C</div>
            <div class="weather-desc">{{ weather.weatherCondition }}</div>
            <div class="weather-details">
              <span>湿度: {{ weather.humidity }}%</span>
              <span>降雨: {{ weather.rainfall }}mm</span>
              <span v-if="weather.consecutiveHighTempDays > 0">连续高温: {{ weather.consecutiveHighTempDays }}天</span>
            </div>
          </div>
          <div v-else class="loading">加载中...</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="info-card">
          <div slot="header">
            <span>📊 水源使用情况</span>
          </div>
          <div class="water-info">
            <div class="water-bar">
              <div class="water-used" :style="{ width: waterUsagePercent + '%' }"></div>
            </div>
            <div class="water-stats">
              <span>已使用: {{ formatWater(overview.totalWater - overview.remainingWater) }} m³</span>
              <span>使用率: {{ waterUsagePercent }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card class="info-card">
          <div slot="header">
            <span>⚙️ 快捷操作</span>
          </div>
          <div class="quick-actions">
            <el-button type="primary" size="medium" @click="generatePlans">
              <i class="el-icon-set-up"></i> 生成灌溉计划
            </el-button>
            <el-button type="success" size="medium" @click="generateReport">
              <i class="el-icon-document"></i> 生成每日报告
            </el-button>
            <el-button type="warning" size="medium" @click="updateStatus">
              <i class="el-icon-refresh"></i> 更新设备状态
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card class="info-card">
          <div slot="header">
            <span>⚠️ 故障设备</span>
          </div>
          <el-empty v-if="overview.faultDevices == 0" description="暂无故障设备"></el-empty>
          <el-alert
            v-else
            :title="'有 ' + overview.faultDevices + ' 台设备故障，请及时处理'"
            type="error"
            show-icon>
          </el-alert>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="info-card">
          <div slot="header">
            <span>📋 待执行计划</span>
          </div>
          <el-empty v-if="overview.pendingPlans == 0" description="暂无待执行计划"></el-empty>
          <div v-else class="pending-info">
            有 <strong>{{ overview.pendingPlans }}</strong> 个计划等待执行
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card class="info-card">
          <div slot="header">
            <span>🚨 最近故障事件</span>
            <el-button type="text" size="mini" @click="loadFaultEvents" style="float: right;">
              <i class="el-icon-refresh"></i> 刷新
            </el-button>
          </div>
          <el-empty v-if="faultEvents.length === 0" description="暂无故障事件"></el-empty>
          <div v-else class="fault-list">
            <div
              v-for="event in faultEvents.slice(0, 3)"
              :key="event.id"
              class="fault-item"
              :class="{ 'resolved': event.resolved }">
              <div class="fault-icon" :class="event.resolved ? 'success' : 'danger'">
                <i :class="event.resolved ? 'el-icon-circle-check' : 'el-icon-warning'"></i>
              </div>
              <div class="fault-content">
                <div class="fault-title">
                  <span class="device-name">{{ event.deviceName }}</span>
                  <span class="fault-status">
                    <el-tag :type="event.resolved ? 'success' : 'danger'" size="mini">
                      {{ event.resolved ? '已处理' : '待处理' }}
                    </el-tag>
                  </span>
                </div>
                <div class="fault-desc">{{ event.faultMessage }}</div>
                <div class="fault-meta">
                  <span>
                    <i class="el-icon-time"></i>
                    {{ formatTime(event.faultTime) }}
                  </span>
                  <span v-if="event.reallocatedPlansCount > 0" class="realloc-badge">
                    <i class="el-icon-refresh"></i>
                    已重新分配 {{ event.reallocatedPlansCount }} 个计划
                  </span>
                </div>
              </div>
              <div class="fault-action">
                <el-button type="text" size="small" @click="$router.push('/devices')">
                  查看详情 <i class="el-icon-arrow-right"></i>
                </el-button>
              </div>
            </div>
            <div v-if="faultEvents.length > 3" class="view-more">
              <el-button type="text" @click="$router.push('/devices')">
                查看全部 {{ faultEvents.length }} 条记录
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'Dashboard',
  data() {
    return {
      overview: {},
      weather: {},
      faultEvents: []
    }
  },
  computed: {
    waterUsagePercent() {
      if (!this.overview.totalWater) return 0
      return Math.round((1 - this.overview.remainingWater / this.overview.totalWater) * 100)
    }
  },
  mounted() {
    this.loadData()
    this.loadFaultEvents()
    this.timer = setInterval(this.loadData, 10000)
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    formatWater(val) {
      if (!val) return '0'
      return val.toFixed(0)
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
    loadData() {
      this.$axios.get('/api/system/overview').then(res => {
        if (res.data.success) {
          this.overview = res.data.data
          this.weather = res.data.data.weather || {}
        }
      })
    },
    simulateWeather() {
      this.$axios.post('/api/system/weather/simulate').then(res => {
        if (res.data.success) {
          this.$message.success('天气模拟完成')
          this.loadData()
        }
      })
    },
    generatePlans() {
      this.$axios.post('/api/plans/generate').then(res => {
        if (res.data.success) {
          this.$message.success(`生成了 ${res.data.count} 个灌溉计划`)
          this.loadData()
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    generateReport() {
      this.$axios.post('/api/system/daily-report/generate').then(res => {
        if (res.data.success) {
          this.$message.success('报告生成成功')
          this.$router.push('/report')
        }
      })
    },
    updateStatus() {
      this.$axios.post('/api/system/update-status').then(res => {
        if (res.data.success) {
          this.$message.success('状态已更新')
          this.loadData()
        }
      })
    }
  }
}
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #303133;
}

.stat-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.stat-content {
  display: flex;
  align-items: center;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  margin-right: 15px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.info-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.weather-info {
  text-align: center;
  padding: 20px;
}

.weather-temp {
  font-size: 48px;
  font-weight: bold;
  color: #409EFF;
}

.weather-desc {
  font-size: 18px;
  color: #606266;
  margin: 10px 0;
}

.weather-details {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 14px;
  color: #909399;
}

.water-info {
  padding: 10px 0;
}

.water-bar {
  height: 20px;
  background: #EBEEF5;
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 10px;
}

.water-used {
  height: 100%;
  background: linear-gradient(90deg, #67C23A, #409EFF);
  border-radius: 10px;
  transition: width 0.3s;
}

.water-stats {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #606266;
}

.quick-actions {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.pending-info {
  padding: 20px;
  text-align: center;
  font-size: 16px;
  color: #606266;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.fault-list {
  max-height: 300px;
  overflow-y: auto;
}

.fault-item {
  display: flex;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #EBEEF5;
  transition: all 0.3s;
}

.fault-item:hover {
  background: #F5F7FA;
}

.fault-item:last-child {
  border-bottom: none;
}

.fault-item.resolved {
  opacity: 0.7;
}

.fault-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
  font-size: 18px;
  color: white;
  flex-shrink: 0;
}

.fault-icon.danger {
  background: linear-gradient(135deg, #F56C6C, #f78989);
}

.fault-icon.success {
  background: linear-gradient(135deg, #67C23A, #95d475);
}

.fault-content {
  flex: 1;
  min-width: 0;
}

.fault-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.device-name {
  font-weight: bold;
  color: #303133;
  font-size: 14px;
}

.fault-desc {
  font-size: 13px;
  color: #606266;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fault-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 15px;
}

.fault-meta .realloc-badge {
  color: #409EFF;
  font-weight: bold;
}

.fault-action {
  flex-shrink: 0;
}

.view-more {
  text-align: center;
  padding: 10px;
  border-top: 1px solid #EBEEF5;
}
</style>
