<template>
  <div class="group-admin">
    <div class="head">
      <div>
        <h2 class="head-title">小组管理</h2>
        <p class="head-sub">查看全部小组，可以代建小组、直接把人拉进组（不需要对方同意）</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreate">代建小组</el-button>
    </div>

    <el-card shadow="never" class="list-card">
      <el-table v-loading="loading" :data="list" style="width: 100%">
        <el-table-column prop="name" label="小组名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="组长" width="130">
          <template #default="{ row }">{{ row.leaderName || '—' }}</template>
        </el-table-column>
        <el-table-column label="项目" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link v-if="row.articleId" type="primary" @click="goProject(row.articleId)">
              {{ row.articleTitle || '查看项目' }}
            </el-link>
            <span v-else class="text-sub">未绑定项目</span>
          </template>
        </el-table-column>
        <el-table-column label="成员" width="90">
          <template #default="{ row }">
            {{ (row.members || []).length }} 人
            <span v-if="(row.pendingMembers || []).length" class="pending-count">
              (+{{ row.pendingMembers.length }} 待同意)
            </span>
          </template>
        </el-table-column>
        <el-table-column label="答辩意见" width="100">
          <template #default="{ row }">{{ row.opinionCount || 0 }} 条</template>
        </el-table-column>
        <el-table-column label="可见范围" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.opinionScopeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="建组时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDetail(row)">成员</el-button>
            <el-button size="small" type="primary" link @click="openAddMember(row)">拉人进组</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !list.length" description="还没有任何小组" />
    </el-card>

    <!-- 代建小组 -->
    <el-dialog v-model="createVisible" title="代建小组" width="560px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="96px">
        <el-form-item label="小组名称" prop="name">
          <el-input v-model="createForm.name" maxlength="100" placeholder="如：智能浇花小分队" />
        </el-form-item>
        <el-form-item label="组长" prop="leaderId">
          <el-select
            v-model="createForm.leaderId"
            filterable
            remote
            :remote-method="searchForLeader"
            :loading="searching"
            placeholder="输入账号或昵称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.nickname || u.account}（${u.account}）`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="小组简介">
          <el-input v-model="createForm.intro" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
        <el-form-item label="答辩意见">
          <el-select v-model="createForm.opinionScope" style="width: 100%">
            <el-option
              v-for="s in scopeOptions"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="form-tip">
            管理员代建时，如果组长是别人，该用户会成为组长，你自己不进组。
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 拉人进组 -->
    <el-dialog v-model="addVisible" title="直接拉人进组" width="560px">
      <p class="dialog-tip">
        管理员拉人不需要对方同意，直接进组。用于特殊情况（如已确认过的人员、补录）。
      </p>
      <el-form label-width="96px">
        <el-form-item label="目标小组">
          <el-input :model-value="currentGroup?.name" disabled />
        </el-form-item>
        <el-form-item label="拉谁进来">
          <el-select
            v-model="addForm.userIds"
            multiple
            filterable
            remote
            :remote-method="searchUsers"
            :loading="searching"
            placeholder="输入账号或昵称搜索"
            style="width: 100%"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.id"
              :label="`${u.nickname || u.account}（${u.account}）`"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="身份">
          <el-radio-group v-model="addForm.memberRole">
            <el-radio :value="2">组员（学生）</el-radio>
            <el-radio :value="3">指导老师</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAdd">加入小组</el-button>
      </template>
    </el-dialog>

    <!-- 成员详情 -->
    <el-dialog v-model="detailVisible" :title="currentGroup?.name || '小组成员'" width="640px">
      <el-descriptions :column="2" border class="detail-desc">
        <el-descriptions-item label="组长">{{ currentGroup?.leaderName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="成员数">{{ (currentGroup?.members || []).length }} 人</el-descriptions-item>
        <el-descriptions-item label="答辩意见可见范围">{{ currentGroup?.opinionScopeName }}</el-descriptions-item>
        <el-descriptions-item label="建组时间">{{ formatTime(currentGroup?.createTime) }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="detail-sub">已加入成员</h4>
      <el-table :data="currentGroup?.members || []" size="small">
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickName || row.account || '—' }}</template>
        </el-table-column>
        <el-table-column prop="memberRoleName" label="角色" width="100" />
        <el-table-column prop="joinTypeName" label="进组方式" width="110" />
        <el-table-column label="加入时间" width="150">
          <template #default="{ row }">{{ formatTime(row.joinTime) }}</template>
        </el-table-column>
      </el-table>

      <template v-if="(currentGroup?.pendingMembers || []).length">
        <h4 class="detail-sub">待同意（{{ currentGroup.pendingMembers.length }}）</h4>
        <el-table :data="currentGroup.pendingMembers" size="small">
          <el-table-column label="昵称" min-width="120">
            <template #default="{ row }">{{ row.nickName || row.account || '—' }}</template>
          </el-table-column>
          <el-table-column prop="memberRoleName" label="角色" width="100" />
          <el-table-column prop="statusName" label="状态" width="100" />
        </el-table>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 小组管理页（管理员）。
 *
 * 管理员在这个页面上能：
 *   1. 看全部小组和成员构成
 *   2. 代建小组（指定组长，组长可以不是自己）
 *   3. 直接拉人进组 —— 不需要对方同意，这是管理员和组长的区别
 */
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminListGroups, adminCreateGroup, adminAddMembers } from '@/api/group'
import { searchUserList } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const searching = ref(false)
const list = ref([])
const userOptions = ref([])

const createVisible = ref(false)
const createRef = ref()
const createForm = reactive({ name: '', leaderId: null, intro: '', opinionScope: 1 })

const addVisible = ref(false)
const addForm = reactive({ userIds: [], memberRole: 2 })

const detailVisible = ref(false)
const currentGroup = ref(null)

const createRules = {
  name: [{ required: true, message: '请填写小组名称', trigger: 'blur' }],
  leaderId: [{ required: true, message: '请指定组长', trigger: 'change' }]
}

const scopeOptions = [
  { value: 0, label: '仅组内可见' },
  { value: 1, label: '组外可见摘要' },
  { value: 2, label: '组外可见全部' }
]

async function load() {
  loading.value = true
  try {
    const res = await adminListGroups()
    list.value = res?.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function searchUsers(keyword) {
  if (!keyword) {
    userOptions.value = []
    return
  }
  searching.value = true
  try {
    const res = await searchUserList({ keyword, pageNum: 1, pageSize: 20 })
    userOptions.value = res?.data?.records || res?.data || []
  } catch (e) {
    userOptions.value = []
  } finally {
    searching.value = false
  }
}

const searchForLeader = searchUsers

function openCreate() {
  createForm.name = ''
  createForm.leaderId = null
  createForm.intro = ''
  createForm.opinionScope = 1
  userOptions.value = []
  createVisible.value = true
}

async function submitCreate() {
  const valid = await createRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await adminCreateGroup({ ...createForm, leaderId: Number(createForm.leaderId) })
    ElMessage.success('小组创建成功')
    createVisible.value = false
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function openAddMember(row) {
  currentGroup.value = row
  addForm.userIds = []
  addForm.memberRole = 2
  userOptions.value = []
  addVisible.value = true
}

async function submitAdd() {
  if (!addForm.userIds.length) {
    ElMessage.warning('请先选择要拉进组的人')
    return
  }
  saving.value = true
  try {
    await adminAddMembers(
      currentGroup.value.id,
      addForm.userIds.map(Number),
      addForm.memberRole
    )
    ElMessage.success('已加入小组')
    addVisible.value = false
    load()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function openDetail(row) {
  currentGroup.value = row
  detailVisible.value = true
}

function goProject(articleId) {
  router.push(`/cooperation/detail/${articleId}`)
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(load)
</script>

<style scoped>
.group-admin {
  padding: 4px;
}

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.head-title {
  font-size: 20px;
  color: #1d3a6b;
  margin: 0 0 4px;
}

.head-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.list-card {
  border-radius: 10px;
}

.text-sub {
  color: #a0a4ab;
  font-size: 12px;
}

.pending-count {
  color: #e6a23c;
  font-size: 12px;
}

.form-tip,
.dialog-tip {
  font-size: 12px;
  color: #a0a4ab;
  line-height: 1.6;
}

.dialog-tip {
  margin: 0 0 14px;
}

.detail-desc {
  margin-bottom: 18px;
}

.detail-sub {
  font-size: 14px;
  color: #1d3a6b;
  margin: 16px 0 10px;
}
</style>
