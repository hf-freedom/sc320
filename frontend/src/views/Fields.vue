<template>
  <div class="fields">
    <h2 class="page-title">地块管理</h2>

    <el-card>
      <div slot="header" class="card-header">
        <span>农田地块列表</span>
        <el-button type="primary" @click="showAddDialog">
          <i class="el-icon-plus"></i> 新增地块
        </el-button>
      </div>

      <el-table :data="fields" border stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="120"></el-table-column>
        <el-table-column prop="name" label="地块名称" width="140"></el-table-column>
        <el-table-column prop="area" label="面积(亩)" width="100">
          <template slot-scope="scope">
            {{ scope.row.area }}
          </template>
        </el-table-column>
        <el-table-column prop="cropType" label="作物类型" width="120">
          <template slot-scope="scope">
            {{ getCropName(scope.row.cropType) }}
          </template>
        </el-table-column>
        <el-table-column prop="growthStage" label="生长期" width="120">
          <template slot-scope="scope">
            <el-tag :type="getStageType(scope.row.growthStage)">
              {{ getStageName(scope.row.growthStage) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="用水阈值" width="140">
          <template slot-scope="scope">
            <div class="threshold-info">
              <div class="threshold-value">
                <span class="label">目标:</span>
                <span class="value">{{ (getThreshold(scope.row.growthStage) * 100).toFixed(0) }}%</span>
              </div>
              <div class="threshold-gap" :class="getGapClass(scope.row.soilMoisture, scope.row.growthStage)">
                {{ getGapText(scope.row.soilMoisture, scope.row.growthStage) }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="soilMoisture" label="土壤湿度" width="160">
          <template slot-scope="scope">
            <div class="moisture-with-threshold">
              <el-progress
                :percentage="Math.round(scope.row.soilMoisture * 100)"
                :color="getMoistureColor(scope.row.soilMoisture, scope.row.growthStage)">
              </el-progress>
              <div class="threshold-marker" :style="{ left: getThreshold(scope.row.growthStage) * 100 + '%' }">
                <span class="marker-line"></span>
                <span class="marker-label">阈值</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cropValue" label="作物价值(元)" width="120"></el-table-column>
        <el-table-column prop="level" label="地块等级" width="100">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.level === 1" type="danger">一等</el-tag>
            <el-tag v-else-if="scope.row.level === 2" type="warning">二等</el-tag>
            <el-tag v-else type="info">三等</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" width="100"></el-table-column>
        <el-table-column label="灌溉策略" width="220">
          <template slot-scope="scope">
            <div class="strategy-info">
              <div class="strategy-tags">
                <el-tag v-if="getStrategy(scope.row).urgent" type="danger" size="mini">紧急</el-tag>
                <el-tag v-else-if="getStrategy(scope.row).needIrrigation" type="warning" size="mini">需灌溉</el-tag>
                <el-tag v-else type="success" size="mini">正常</el-tag>
                <el-tag size="mini" type="info">{{ getStrategy(scope.row).priority }}优先</el-tag>
              </div>
              <div class="strategy-desc">{{ getStrategy(scope.row).description }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="primary" @click="showEditDialog(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteField(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="地块名称">
          <el-input v-model="form.name" placeholder="请输入地块名称"></el-input>
        </el-form-item>
        <el-form-item label="面积(亩)">
          <el-input-number v-model="form.area" :min="0.1" :step="0.1" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="作物类型">
          <el-select v-model="form.cropType" placeholder="请选择作物类型">
            <el-option label="水稻" value="RICE"></el-option>
            <el-option label="小麦" value="WHEAT"></el-option>
            <el-option label="玉米" value="CORN"></el-option>
            <el-option label="棉花" value="COTTON"></el-option>
            <el-option label="大豆" value="SOYBEAN"></el-option>
            <el-option label="蔬菜" value="VEGETABLE"></el-option>
            <el-option label="果树" value="FRUIT"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="生长期">
          <el-select v-model="form.growthStage" placeholder="请选择生长期">
            <el-option label="苗期" value="SEEDLING"></el-option>
            <el-option label="营养生长期" value="VEGETATIVE"></el-option>
            <el-option label="开花期" value="FLOWERING"></el-option>
            <el-option label="结果期" value="FRUITING"></el-option>
            <el-option label="成熟期" value="RIPENING"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="土壤湿度(%)">
          <el-input-number v-model="form.soilMoisture" :min="0" :max="1" :step="0.01" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="作物价值(元)">
          <el-input-number v-model="form.cropValue" :min="0" :step="100" controls-position="right"></el-input-number>
        </el-form-item>
        <el-form-item label="地块等级">
          <el-select v-model="form.level">
            <el-option :label="1" :value="1"></el-option>
            <el-option :label="2" :value="2"></el-option>
            <el-option :label="3" :value="3"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="请输入位置"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="saveField">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
const cropNames = {
  RICE: '水稻', WHEAT: '小麦', CORN: '玉米', COTTON: '棉花',
  SOYBEAN: '大豆', VEGETABLE: '蔬菜', FRUIT: '果树'
}

const stageNames = {
  SEEDLING: '苗期', VEGETATIVE: '营养生长期', FLOWERING: '开花期',
  FRUITING: '结果期', RIPENING: '成熟期'
}

const stageThresholds = {
  SEEDLING: 0.3, VEGETATIVE: 0.6, FLOWERING: 0.9, FRUITING: 0.8, RIPENING: 0.4
}

export default {
  name: 'Fields',
  data() {
    return {
      fields: [],
      dialogVisible: false,
      isEdit: false,
      form: {
        id: '',
        name: '',
        area: 1.0,
        cropType: 'RICE',
        growthStage: 'VEGETATIVE',
        soilMoisture: 0.5,
        cropValue: 5000,
        level: 1,
        location: ''
      }
    }
  },
  computed: {
    dialogTitle() {
      return this.isEdit ? '编辑地块' : '新增地块'
    }
  },
  mounted() {
    this.loadFields()
  },
  methods: {
    getCropName(type) {
      return cropNames[type] || type
    },
    getStageName(stage) {
      return stageNames[stage] || stage
    },
    getStageType(stage) {
      if (stage === 'FLOWERING' || stage === 'FRUITING') return 'danger'
      if (stage === 'VEGETATIVE') return 'warning'
      return 'success'
    },
    getMoistureColor(moisture, stage) {
      const threshold = stageThresholds[stage] || 0.5
      if (moisture < threshold * 0.6) return '#F56C6C'
      if (moisture < threshold * 0.8) return '#E6A23C'
      return '#67C23A'
    },
    getThreshold(stage) {
      return stageThresholds[stage] || 0.5
    },
    getGapClass(moisture, stage) {
      const threshold = stageThresholds[stage] || 0.5
      const gap = threshold - moisture
      if (gap > 0.3) return 'gap-danger'
      if (gap > 0.1) return 'gap-warning'
      if (gap > 0) return 'gap-normal'
      return 'gap-sufficient'
    },
    getGapText(moisture, stage) {
      const threshold = stageThresholds[stage] || 0.5
      const gap = threshold - moisture
      if (gap > 0) {
        return '缺水 ' + (gap * 100).toFixed(0) + '%'
      }
      return '充足 +' + Math.abs(gap * 100).toFixed(0) + '%'
    },
    getStrategy(field) {
      const threshold = stageThresholds[field.growthStage] || 0.5
      const deficit = threshold - field.soilMoisture
      const result = {
        urgent: false,
        needIrrigation: false,
        priority: '低',
        description: ''
      }

      if (deficit > 0.3) {
        result.urgent = true
        result.needIrrigation = true
        result.priority = '高'
        result.description = '严重缺水，需立即灌溉'
      } else if (deficit > 0.15) {
        result.needIrrigation = true
        result.priority = '中'
        result.description = '中度缺水，建议尽快灌溉'
      } else if (deficit > 0) {
        result.needIrrigation = true
        result.priority = '低'
        result.description = '轻度缺水，可择机灌溉'
      } else {
        result.description = '水分充足，无需灌溉'
      }

      if (field.growthStage === 'FLOWERING' || field.growthStage === 'FRUITING') {
        if (deficit > 0) {
          result.priority = '高'
          result.description += '（关键生长期）'
        }
      }

      if (field.level === 1) {
        result.priority = result.priority === '低' ? '中' : result.priority
      }

      return result
    },
    loadFields() {
      this.$axios.get('/api/fields').then(res => {
        if (res.data.success) {
          this.fields = res.data.data
        }
      })
    },
    showAddDialog() {
      this.isEdit = false
      this.form = {
        id: '',
        name: '',
        area: 1.0,
        cropType: 'RICE',
        growthStage: 'VEGETATIVE',
        soilMoisture: 0.5,
        cropValue: 5000,
        level: 1,
        location: ''
      }
      this.dialogVisible = true
    },
    showEditDialog(row) {
      this.isEdit = true
      this.form = { ...row }
      this.dialogVisible = true
    },
    saveField() {
      if (!this.form.name) {
        this.$message.warning('请输入地块名称')
        return
      }
      const request = this.isEdit
        ? this.$axios.put('/api/fields/' + this.form.id, this.form)
        : this.$axios.post('/api/fields', this.form)
      
      request.then(res => {
        if (res.data.success) {
          this.$message.success(this.isEdit ? '更新成功' : '添加成功')
          this.dialogVisible = false
          this.loadFields()
        } else {
          this.$message.error(res.data.message)
        }
      })
    },
    deleteField(id) {
      this.$confirm('确定要删除该地块吗?', '提示', {
        type: 'warning'
      }).then(() => {
        this.$axios.delete('/api/fields/' + id).then(res => {
          if (res.data.success) {
            this.$message.success('删除成功')
            this.loadFields()
          } else {
            this.$message.error(res.data.message)
          }
        })
      })
    }
  }
}
</script>

<style scoped>
.fields {
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

.threshold-info {
  line-height: 1.4;
}

.threshold-value {
  font-size: 14px;
  margin-bottom: 4px;
}

.threshold-value .label {
  color: #909399;
  margin-right: 4px;
}

.threshold-value .value {
  font-weight: bold;
  color: #409EFF;
}

.threshold-gap {
  font-size: 12px;
  font-weight: bold;
}

.gap-danger {
  color: #F56C6C;
}

.gap-warning {
  color: #E6A23C;
}

.gap-normal {
  color: #909399;
}

.gap-sufficient {
  color: #67C23A;
}

.moisture-with-threshold {
  position: relative;
  padding-top: 5px;
}

.threshold-marker {
  position: absolute;
  top: 0;
  transform: translateX(-50%);
  text-align: center;
}

.threshold-marker .marker-line {
  display: block;
  width: 2px;
  height: 8px;
  background: #F56C6C;
  margin: 0 auto;
}

.threshold-marker .marker-label {
  font-size: 10px;
  color: #F56C6C;
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  white-space: nowrap;
}

.strategy-info {
  line-height: 1.4;
}

.strategy-tags {
  margin-bottom: 4px;
}

.strategy-tags .el-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.strategy-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.3;
}
</style>
