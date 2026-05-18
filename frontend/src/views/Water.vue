<template>
  <div class="water">
    <h2 class="page-title">水源监控</h2>

    <el-row :gutter="20">
      <el-col :span="12" v-for="ws in waterSources" :key="ws.id">
        <el-card class="water-card">
          <div slot="header">
            <span>💧 {{ ws.name }}</span>
            <el-button type="text" @click="showEditDialog(ws)">编辑</el-button>
          </div>
          <div class="water-content">
            <div class="water-circle">
              <svg width="180" height="180" viewBox="0 0 180 180">
                <circle
                  cx="90" cy="90" r="75"
                  fill="none"
                  stroke="#EBEEF5"
                  stroke-width="12">
                </circle>
                <circle
                  cx="90" cy="90" r="75"
                  fill="none"
                  :stroke="getUsageColor(ws)"
                  stroke-width="12"
                  :stroke-dasharray="getStrokeDasharray(ws)"
                  stroke-dashoffset="0"
                  stroke-linecap="round"
                  transform="rotate(-90 90 90)">
                </circle>
              </svg>
              <div class="water-percent">
                {{ getUsagePercent(ws) }}%
              </div>
            </div>
            <div class="water-details">
              <div class="detail-item">
                <span class="label">总容量:</span>
                <span class="value">{{ ws.totalCapacity }} m³</span>
              </div>
              <div class="detail-item">
                <span class="label">剩余量:</span>
                <span class="value remaining">{{ ws.remainingCapacity.toFixed(1) }} m³</span>
              </div>
              <div class="detail-item">
                <span class="label">已使用:</span>
                <span class="value used">{{ (ws.totalCapacity - ws.remainingCapacity).toFixed(1) }} m³</span>
              </div>
              <div class="detail-item">
                <span class="label">日供水上限:</span>
                <span class="value">{{ ws.dailySupplyLimit }} m³</span>
              </div>
              <div class="detail-item">
                <span class="label">更新时间:</span>
                <span class="value">{{ formatTime(ws.updateTime) }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px;">
      <div slot="header">
        <span>⚠️ 水源分配规则</span>
      </div>
      <div class="rules">
        <el-steps :active="0" finish-status="success" simple>
          <el-step title="作物价值优先"></el-step>
          <el-step title="缺水程度评估"></el-step>
          <el-step title="地块等级排序"></el-step>
          <el-step title="智能水量分配"></el-step>
        </el-steps>
        <div class="rule-desc">
          <p>当水源不足时，系统将按照以下优先级进行水量分配：</p>
          <ol>
            <li><strong>作物价值：</strong>高价值作物优先获得灌溉</li>
            <li><strong>缺水程度：</strong>土壤湿度低于阈值最多的地块优先</li>
            <li><strong>地块等级：</strong>高等级地块享有优先灌溉权</li>
          </ol>
        </div>
      </div>
    </el-card>

    <el-dialog title="编辑水源信息" :visible.sync="dialogVisible" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="水源名称">
          <el-input v-model="form.name"></el-input>
        </el-form-item>
        <el-form-item label="总容量(m³)">
          <el-input-number v-model="form.totalCapacity" :min="1" :step="10" controls-position="right" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="剩余量(m³)">
          <el-input-number v-model="form.remainingCapacity" :min="0" :max="form.totalCapacity" :step="10" controls-position="right" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="日供水上限(m³)">
          <el-input-number v-model="form.dailySupplyLimit" :min="1" :step="10" controls-position="right" style="width: 100%;"></el-input-number>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveWaterSource">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'Water',
  data() {
    return {
      waterSources: [],
      dialogVisible: false,
      form: {
        id: '',
        name: '',
        totalCapacity: 0,
        remainingCapacity: 0,
        dailySupplyLimit: 0
      }
    }
  },
  mounted() {
    this.loadData()
    this.timer = setInterval(this.loadData, 5000)
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    formatTime(time) {
      if (!time) return '-'
      return new Date(time).toLocaleString('zh-CN')
    },
    getUsagePercent(ws) {
      if (!ws.totalCapacity) return 0
      return Math.round((1 - ws.remainingCapacity / ws.totalCapacity) * 100)
    },
    getUsageColor(ws) {
      const percent = this.getUsagePercent(ws)
      if (percent > 80) return '#F56C6C'
      if (percent > 60) return '#E6A23C'
      return '#409EFF'
    },
    getStrokeDasharray(ws) {
      const percent = this.getUsagePercent(ws)
      const circumference = 2 * Math.PI * 75
      const used = circumference * percent / 100
      return `${used} ${circumference}`
    },
    loadData() {
      this.$axios.get('/api/system/water-sources').then(res => {
        if (res.data.success) {
          this.waterSources = res.data.data
        }
      })
    },
    showEditDialog(ws) {
      this.form = { ...ws }
      this.dialogVisible = true
    },
    saveWaterSource() {
      this.$axios.put('/api/system/water-sources/' + this.form.id, this.form).then(res => {
        if (res.data.success) {
          this.$message.success('更新成功')
          this.dialogVisible = false
          this.loadData()
        }
      })
    }
  }
}
</script>

<style scoped>
.water {
  padding: 0;
}

.page-title {
  font-size: 24px;
  margin-bottom: 20px;
  color: #303133;
}

.water-card {
  border-radius: 8px;
  border: none;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.water-content {
  display: flex;
  align-items: center;
  gap: 30px;
}

.water-circle {
  position: relative;
  width: 180px;
  height: 180px;
}

.water-percent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.water-details {
  flex: 1;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.detail-item:last-child {
  border-bottom: none;
}

.label {
  color: #909399;
}

.value {
  font-weight: bold;
  color: #303133;
}

.value.remaining {
  color: #67C23A;
}

.value.used {
  color: #E6A23C;
}

.rules {
  padding: 20px 0;
}

.rule-desc {
  margin-top: 30px;
  padding: 20px;
  background: #F5F7FA;
  border-radius: 8px;
}

.rule-desc p {
  margin-bottom: 10px;
  font-weight: bold;
  color: #606266;
}

.rule-desc ol {
  padding-left: 20px;
}

.rule-desc li {
  margin: 8px 0;
  color: #606266;
}
</style>
