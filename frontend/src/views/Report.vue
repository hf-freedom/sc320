<template>
  <div class="report">
    <h2 class="page-title">每日报告</h2>

    <el-card>
      <div slot="header" class="card-header">
        <span>📊 今日运行报告</span>
        <el-button type="primary" @click="generateReport">
          <i class="el-icon-refresh"></i> 重新生成
        </el-button>
      </div>

      <div v-if="report" class="report-content">
        <div class="report-date">
          <i class="el-icon-time"></i>
          报告日期: {{ report.date }}
          <span style="margin-left: 20px; color: #909399; font-size: 14px;">
            生成时间: {{ report.generatedTime }}
          </span>
        </div>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="8">
            <el-card class="report-card" shadow="hover">
              <div slot="header" class="card-subtitle">
                <i class="el-icon-location-outline"></i> 地块缺水风险
              </div>
              <div class="risk-stats">
                <div class="risk-item high">
                  <span class="risk-label">高风险</span>
                  <span class="risk-value">{{ report.highRiskFields }}</span>
                </div>
                <div class="risk-item medium">
                  <span class="risk-label">中风险</span>
                  <span class="risk-value">{{ report.mediumRiskFields }}</span>
                </div>
                <div class="risk-item low">
                  <span class="risk-label">低风险</span>
                  <span class="risk-value">{{ report.lowRiskFields }}</span>
                </div>
              </div>
              <div class="chart-container">
                <canvas ref="riskChart" width="300" height="200"></canvas>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="report-card" shadow="hover">
              <div slot="header" class="card-subtitle">
                <i class="el-icon-cpu"></i> 设备负载情况
              </div>
              <div class="device-stats">
                <div class="stat-row">
                  <span>设备总数:</span>
                  <span class="stat-value">{{ report.totalDevices }} 台</span>
                </div>
                <div class="stat-row">
                  <span>运行中:</span>
                  <span class="stat-value running">{{ report.runningDevices }} 台</span>
                </div>
                <div class="stat-row">
                  <span>设备负载率:</span>
                  <span class="stat-value" :class="getLoadClass(report.deviceLoadRate)">
                    {{ (report.deviceLoadRate * 100).toFixed(1) }}%
                  </span>
                </div>
              </div>
              <div class="chart-container">
                <canvas ref="deviceChart" width="300" height="200"></canvas>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="report-card" shadow="hover">
              <div slot="header" class="card-subtitle">
                <i class="el-icon-umbrella"></i> 水源使用情况
              </div>
              <div class="water-stats">
                <div class="stat-row">
                  <span>剩余水量:</span>
                  <span class="stat-value remaining">{{ report.waterRemaining.toFixed(1) }} m³</span>
                </div>
                <div class="stat-row">
                  <span>今日用水量:</span>
                  <span class="stat-value used">{{ report.waterUsageToday.toFixed(1) }} m³</span>
                </div>
                <div class="stat-row">
                  <span>总地块数:</span>
                  <span class="stat-value">{{ report.totalFields }} 块</span>
                </div>
              </div>
              <div class="chart-container">
                <canvas ref="waterChart" width="300" height="200"></canvas>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card style="margin-top: 20px;">
          <div slot="header" class="card-subtitle">
            <i class="el-icon-lightbulb"></i> 系统建议
          </div>
          <div class="suggestions">
            <el-alert
              v-for="(sugg, index) in suggestions"
              :key="index"
              :title="sugg"
              type="info"
              show-icon
              :closable="false"
              style="margin-bottom: 10px;">
            </el-alert>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="暂无报告数据"></el-empty>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'Report',
  data() {
    return {
      report: null,
      suggestions: []
    }
  },
  mounted() {
    this.loadReport()
  },
  methods: {
    getLoadClass(rate) {
      if (rate > 0.8) return 'danger'
      if (rate > 0.6) return 'warning'
      return 'success'
    },
    loadReport() {
      this.$axios.get('/api/system/daily-report').then(res => {
        if (res.data.success) {
          this.report = res.data.data
          this.generateSuggestions()
          this.$nextTick(() => {
            this.drawCharts()
          })
        }
      })
    },
    generateReport() {
      this.$axios.post('/api/system/daily-report/generate').then(res => {
        if (res.data.success) {
          this.$message.success('报告生成成功')
          this.report = res.data.data
          this.generateSuggestions()
          this.$nextTick(() => {
            this.drawCharts()
          })
        }
      })
    },
    generateSuggestions() {
      this.suggestions = []
      if (this.report.highRiskFields > 0) {
        this.suggestions.push(`当前有 ${this.report.highRiskFields} 块地块处于高缺水风险，建议优先安排灌溉`)
      }
      if (this.report.deviceLoadRate > 0.8) {
        this.suggestions.push('设备负载率较高，建议合理安排灌溉时间或增加设备')
      }
      if (this.report.waterRemaining < 200) {
        this.suggestions.push('水源余量不足，建议启动水源分配优先级机制')
      }
      if (this.suggestions.length === 0) {
        this.suggestions.push('系统运行正常，所有指标良好')
      }
    },
    drawCharts() {
      this.drawRiskChart()
      this.drawDeviceChart()
      this.drawWaterChart()
    },
    drawRiskChart() {
      const canvas = this.$refs.riskChart
      if (!canvas) return
      const ctx = canvas.getContext('2d')
      const total = this.report.highRiskFields + this.report.mediumRiskFields + this.report.lowRiskFields
      if (total === 0) return

      const data = [
        { value: this.report.highRiskFields, color: '#F56C6C', label: '高风险' },
        { value: this.report.mediumRiskFields, color: '#E6A23C', label: '中风险' },
        { value: this.report.lowRiskFields, color: '#67C23A', label: '低风险' }
      ]

      let startAngle = -Math.PI / 2
      const centerX = 150, centerY = 100, radius = 70

      data.forEach(item => {
        const sliceAngle = (item.value / total) * 2 * Math.PI
        ctx.beginPath()
        ctx.moveTo(centerX, centerY)
        ctx.arc(centerX, centerY, radius, startAngle, startAngle + sliceAngle)
        ctx.closePath()
        ctx.fillStyle = item.color
        ctx.fill()
        startAngle += sliceAngle
      })

      ctx.beginPath()
      ctx.arc(centerX, centerY, 40, 0, 2 * Math.PI)
      ctx.fillStyle = '#fff'
      ctx.fill()

      ctx.fillStyle = '#303133'
      ctx.font = 'bold 16px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(total, centerX, centerY + 6)
      ctx.font = '12px sans-serif'
      ctx.fillText('总地块', centerX, centerY + 22)
    },
    drawDeviceChart() {
      const canvas = this.$refs.deviceChart
      if (!canvas) return
      const ctx = canvas.getContext('2d')

      const width = 280, height = 180
      const barWidth = 80
      const barMaxHeight = 140

      ctx.clearRect(0, 0, width, height)

      const idle = this.report.totalDevices - this.report.runningDevices
      const running = this.report.runningDevices
      const maxVal = Math.max(running, idle, 1)

      const runningHeight = (running / maxVal) * barMaxHeight
      const idleHeight = (idle / maxVal) * barMaxHeight

      ctx.fillStyle = '#67C23A'
      ctx.fillRect(50, height - runningHeight - 20, barWidth, runningHeight)
      ctx.fillStyle = '#909399'
      ctx.fillRect(170, height - idleHeight - 20, barWidth, idleHeight)

      ctx.fillStyle = '#606266'
      ctx.font = '12px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(`运行中: ${running}`, 90, height - 5)
      ctx.fillText(`空闲: ${idle}`, 210, height - 5)
    },
    drawWaterChart() {
      const canvas = this.$refs.waterChart
      if (!canvas) return
      const ctx = canvas.getContext('2d')

      const total = this.report.waterRemaining + this.report.waterUsageToday
      if (total === 0) return

      const usedPercent = this.report.waterUsageToday / total

      const centerX = 150, centerY = 100, radius = 70

      ctx.beginPath()
      ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI)
      ctx.fillStyle = '#EBEEF5'
      ctx.fill()

      ctx.beginPath()
      ctx.moveTo(centerX, centerY)
      ctx.arc(centerX, centerY, radius, -Math.PI / 2, -Math.PI / 2 + usedPercent * 2 * Math.PI)
      ctx.closePath()
      ctx.fillStyle = '#409EFF'
      ctx.fill()

      ctx.beginPath()
      ctx.arc(centerX, centerY, 40, 0, 2 * Math.PI)
      ctx.fillStyle = '#fff'
      ctx.fill()

      ctx.fillStyle = '#303133'
      ctx.font = 'bold 16px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(`${(usedPercent * 100).toFixed(1)}%`, centerX, centerY + 6)
      ctx.font = '12px sans-serif'
      ctx.fillText('使用率', centerX, centerY + 22)
    }
  }
}
</script>

<style scoped>
.report {
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

.report-date {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
  padding: 10px 0;
  border-bottom: 2px solid #409EFF;
}

.card-subtitle {
  font-size: 16px;
  font-weight: bold;
  color: #606266;
}

.report-card {
  height: 100%;
}

.risk-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 15px;
}

.risk-item {
  text-align: center;
}

.risk-label {
  display: block;
  font-size: 12px;
  color: #909399;
  margin-bottom: 5px;
}

.risk-value {
  font-size: 24px;
  font-weight: bold;
}

.risk-item.high .risk-value {
  color: #F56C6C;
}

.risk-item.medium .risk-value {
  color: #E6A23C;
}

.risk-item.low .risk-value {
  color: #67C23A;
}

.chart-container {
  display: flex;
  justify-content: center;
  padding: 10px 0;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.stat-value {
  font-weight: bold;
  color: #303133;
}

.stat-value.running {
  color: #67C23A;
}

.stat-value.remaining {
  color: #67C23A;
}

.stat-value.used {
  color: #E6A23C;
}

.stat-value.danger {
  color: #F56C6C;
}

.stat-value.warning {
  color: #E6A23C;
}

.stat-value.success {
  color: #67C23A;
}

.suggestions {
  margin-top: 10px;
}
</style>
