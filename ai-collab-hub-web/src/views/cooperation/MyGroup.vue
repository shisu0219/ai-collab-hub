<template>
  <div class="group-page">
    <div class="page-container">
      <h2 class="head-title">我的小组</h2>
      <p class="head-sub">一个小组做一个项目，组长可以邀请组员和指导老师</p>

      <!-- 待处理邀请 -->
      <el-alert
        v-if="invites.length"
        class="invite-alert"
        type="warning"
        :closable="false"
      >
        <template #title>
          你有 {{ invites.length }} 条进组邀请待处理
        </template>
        <div class="invite-list">
          <div v-for="inv in invites" :key="inv.id" class="invite-row">
            <span>
              <b>{{ inv.groupName || '某个小组' }}</b>
              邀请你作为
              <el-tag size="small" :type="inv.memberRole === 3 ? 'warning' : 'primary'">
                {{ inv.memberRoleName }}
              </el-tag>
              加入
            </span>
            <span class="invite-actions">
              <el-button size="small" type="primary" @click="handleInvite(inv, 1)">同意</el-button>
              <el-button size="small" @click="handleInvite(inv, 0)">拒绝</el-button>
            </span>
          </div>
        </div>
      </el-alert>

      <div v-loading="loading" class="group-body">
        <!-- 已建组：展示小组详情 -->
        <template v-if="group">
          <el-card class="group-card" shadow="never">
            <div class="group-head">
              <div>
                <h3 class="group-name">{{ group.name }}</h3>
                <p class="group-intro">{{ group.intro || '还没有填小组简介' }}</p>
              </div>
              <div class="group-head-actions">
                <el-button v-if="group.isLeader" size="small" :icon="Edit" @click="openEdit">
                  改组信息
                </el-button>
              </div>
            </div>

            <el-descriptions :column="2" border class="group-desc">
              <el-descriptions-item label="组长">
                {{ group.leaderName || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="项目">
                <template v-if="group.articleId">
                  <el-link type="primary" @click="goProject(group.articleId)">
                    {{ group.articleTitle || '查看项目' }}
                  </el-link>
                </template>
                <template v-else>
                  <span class="text-warn">还没填写项目</span>
                  <el-button
                    v-if="group.isLeader"
                    size="small"
                    type="primary"
                    link
                    @click="goFillProject"
                  >
                    去填写
                  </el-button>
                </template>
              </el-descriptions-item>
              <el-descriptions-item label="答辩意见可见范围">
                {{ group.opinionScopeName }}
                <el-button
                  v-if="group.isLeader"
                  size="small"
                  type="primary"
                  link
                  @click="openScopeDialog"
                >
                  修改
                </el-button>
              </el-descriptions-item>
              <el-descriptions-item label="建组时间">
                {{ formatTime(group.createTime) }}
              </el-descriptions-item>
            </el-descriptions>

            <!-- 成员列表 -->
            <div class="member-section">
              <div class="section-head">
                <h4>组内成员（{{ (group.members || []).length }}）</h4>
                <el-button
                  v-if="group.isLeader"
                  size="small"
                  type="primary"
                  :icon="Plus"
                  @click="openInvite"
                >
                  邀请成员
                </el-button>
              </div>
              <div class="member-grid">
                <div v-for="m in group.members" :key="m.id" class="member-card">
                  <el-avatar :size="38">{{ (m.nickName || 'U').charAt(0) }}</el-avatar>
                  <div class="member-info">
                    <div class="member-name">
                      {{ m.nickName || m.account || '未知用户' }}
                      <el-tag
                        size="small"
                        :type="roleTagType(m.memberRole)"
                        effect="plain"
                      >
                        {{ m.memberRoleName }}
                      </el-tag>
                    </div>
                    <div class="member-meta">
                      {{ m.joinTypeName }} · {{ formatTime(m.joinTime) }}
                    </div>
                  </div>
                  <el-button
                    v-if="group.isLeader && m.memberRole !== 1"
                    size="small"
                    type="danger"
                    link
                    @click="handleRemove(m)"
                  >
                    移出
                  </el-button>
                </div>
              </div>

              <!-- 待同意的人 -->
              <template v-if="(group.pendingMembers || []).length">
                <h4 class="pending-title">等待对方同意（{{ group.pendingMembers.length }}）</h4>
                <div class="pending-list">
                  <el-tag
                    v-for="m in group.pendingMembers"
                    :key="m.id"
                    type="info"
                    effect="plain"
                  >
                    {{ m.nickName || m.account }} · {{ m.memberRoleName }} · 待同意
                  </el-tag>
                </div>
              </template>
            </div>
          </el-card>

          <!-- 答辩意见区块 -->
          <OpinionList
            class="opinion-block"
            :group-id="group.id"
            :is-leader="!!group.isLeader"
            :default-scope="group.opinionScope"
            :members="group.members || []"
          />
        </template>

        <!-- 还没建组 -->
        <el-card v-else-if="!loading" shadow="never" class="empty-card">
          <el-empty description="你还没有小组">
            <el-button type="primary" :icon="Plus" @click="openCreate">创建小组</el-button>
            <p class="empty-tip">一个小组对应一个项目，建好组后就能填写项目信息了</p>
          </el-empty>
        </el-card>
      </div>
    </div>

    <!-- 建组 / 改组弹窗 -->
    <el-dialog v-model="formVisible" :title="form.id ? '修改小组信息' : '创建小组'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="小组名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="如：智能浇花小分队" />
        </el-form-item>
        <el-form-item label="小组简介">
          <el-input
            v-model="form.intro"
            type="textarea"
            :rows="3"
            maxlength="500"
            placeholder="一句话说说你们要做什么"
          />
        </el-form-item>
        <template v-if="!form.id">
          <el-form-item label="答辩意见">
            <el-select v-model="form.opinionScope" style="width: 100%">
              <el-option
                v-for="s in scopeOptions"
                :key="s.value"
                :label="s.label"
                :value="s.value"
              />
            </el-select>
            <div class="form-tip">这是组外人员能看到多少答辩意见的默认设置，之后可以改</div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 邀请成员弹窗 -->
    <el-dialog v-model="inviteVisible" title="邀请成员" width="520px">
      <el-form label-width="96px">
        <el-form-item label="邀请谁">
          <el-select
            v-model="inviteForm.userIds"
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
          <el-radio-group v-model="inviteForm.memberRole">
            <el-radio :value="2">组员（学生）</el-radio>
            <el-radio :value="3">指导老师</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <div class="form-tip">对方同意后才会正式进组，指导老师和组员一样能看全部答辩意见</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inviteVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitInvite">发送邀请</el-button>
      </template>
    </el-dialog>

    <!-- 可见性设置弹窗 -->
    <el-dialog v-model="scopeVisible" title="答辩意见可见范围" width="520px">
      <p class="scope-desc">
        组员和指导老师永远能看全部；这里设置的是<b>组外人员</b>能看到多少。
      </p>
      <el-radio-group v-model="scopeForm.scope" class="scope-group">
        <el-radio v-for="s in scopeOptions" :key="s.value" :value="s.value" class="scope-radio">
          <div class="scope-label">{{ s.label }}</div>
          <div class="scope-sub">{{ s.desc }}</div>
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="scopeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitScope">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * 我的小组页。
 *
 * 一个小组只能有一个项目，所以页面上是「先建组、再填项目」的顺序：
 * 建好组后这里会出现「去填写」的入口，填完项目就绑定到这个组上。
 *
 * 成员分两种进法：组长邀请（对方要同意）、管理员直接拉（不用同意）。
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit } from '@element-plus/icons-vue'
import {
  getMyGroups,
  getGroupDetail,
  createGroup,
  updateGroup,
  inviteMembers,
  handleInvite as apiHandleInvite,
  removeMember,
  getMyInvites
} from '@/api/group'
import { searchUserList } from '@/api/admin'
import OpinionList from '@/components/OpinionList.vue'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const group = ref(null)
const invites = ref([])

const formVisible = ref(false)
const formRef = ref()
const form = ref({ name: '', intro: '', opinionScope: 1 })

const inviteVisible = ref(false)
const inviteForm = ref({ userIds: [], memberRole: 2 })
const userOptions = ref([])
const searching = ref(false)

const scopeVisible = ref(false)
const scopeForm = ref({ scope: 1 })

const rules = {
  name: [{ required: true, message: '请填写小组名称', trigger: 'blur' }]
}

const scopeOptions = [
  { value: 0, label: '仅组内可见', desc: '组外人员看不到任何答辩意见，只知道有多少条' },
  { value: 1, label: '组外可见摘要', desc: '组外人员能看到标题、来源和正文前 50 字' },
  { value: 2, label: '组外可见全部', desc: '组外人员能看到完整内容和附件' }
]

async function loadAll() {
  loading.value = true
  try {
    const res = await getMyGroups()
    const list = res?.data || []
    if (list.length) {
      // 取第一条并加载详情（一个用户一般只带一个组）
      const detail = await getGroupDetail(list[0].id)
      group.value = detail?.data || null
    } else {
      group.value = null
    }
    const inv = await getMyInvites()
    invites.value = inv?.data || []
  } catch (e) {
    group.value = null
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { name: '', intro: '', opinionScope: 1 }
  formVisible.value = true
}

function openEdit() {
  form.value = {
    id: group.value.id,
    name: group.value.name,
    intro: group.value.intro,
    opinionScope: group.value.opinionScope
  }
  formVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await updateGroup({ ...form.value })
      ElMessage.success('保存成功')
    } else {
      await createGroup({ ...form.value })
      ElMessage.success('小组创建成功')
    }
    formVisible.value = false
    loadAll()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function openInvite() {
  inviteForm.value = { userIds: [], memberRole: 2 }
  userOptions.value = []
  inviteVisible.value = true
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

async function submitInvite() {
  if (!inviteForm.value.userIds.length) {
    ElMessage.warning('请先选择要邀请的人')
    return
  }
  saving.value = true
  try {
    await inviteMembers({
      groupId: group.value.id,
      userIds: inviteForm.value.userIds,
      memberRole: inviteForm.value.memberRole
    })
    ElMessage.success('邀请已发出，等待对方同意')
    inviteVisible.value = false
    loadAll()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

async function handleInvite(inv, accept) {
  try {
    await apiHandleInvite({ memberId: inv.id, accept })
    ElMessage.success(accept === 1 ? '已加入小组' : '已拒绝')
    loadAll()
  } catch (e) {
    // 拦截器已提示
  }
}

async function handleRemove(m) {
  try {
    await ElMessageBox.confirm(`确定把「${m.nickName || m.account}」移出小组吗？`, '提示', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await removeMember(group.value.id, m.userId)
    ElMessage.success('已移出小组')
    loadAll()
  } catch (e) {
    // 拦截器已提示
  }
}

function openScopeDialog() {
  scopeForm.value = { scope: group.value.opinionScope ?? 1 }
  scopeVisible.value = true
}

async function submitScope() {
  saving.value = true
  try {
    await updateGroup({
      id: group.value.id,
      name: group.value.name,
      intro: group.value.intro,
      opinionScope: scopeForm.value.scope
    })
    ElMessage.success('设置已保存')
    scopeVisible.value = false
    loadAll()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function goProject(articleId) {
  router.push(`/cooperation/detail/${articleId}`)
}

function goFillProject() {
  router.push({ path: '/cooperation/publish', query: { groupId: group.value.id } })
}

function roleTagType(role) {
  const map = { 1: 'danger', 2: 'primary', 3: 'warning' }
  return map[role] || 'info'
}

function formatTime(t) {
  if (!t) {
    return ''
  }
  return String(t).replace('T', ' ').slice(0, 16)
}

onMounted(loadAll)
</script>

<style scoped>
.group-page {
  padding: 24px 0;
}

.head-title {
  font-size: 22px;
  color: #1d3a6b;
  margin: 0 0 6px;
}

.head-sub {
  font-size: 13px;
  color: #909399;
  margin: 0 0 14px;
}

.invite-alert {
  margin-bottom: 16px;
}

.invite-list {
  margin-top: 8px;
}

.invite-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 0;
}

.invite-actions {
  display: flex;
  gap: 8px;
}

.group-card {
  border-radius: 10px;
}

.group-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.group-name {
  font-size: 18px;
  color: #1d3a6b;
  margin: 0 0 6px;
}

.group-intro {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.group-desc {
  margin-bottom: 20px;
}

.member-section {
  border-top: 1px solid #ebeef5;
  padding-top: 16px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-head h4 {
  margin: 0;
  font-size: 15px;
  color: #1d3a6b;
}

.member-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #303133;
}

.member-meta {
  font-size: 12px;
  color: #a0a4ab;
  margin-top: 2px;
}

.pending-title {
  margin: 18px 0 8px;
  font-size: 14px;
  color: #909399;
}

.pending-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.opinion-block {
  margin-top: 18px;
}

.empty-card {
  border-radius: 10px;
}

.empty-tip {
  font-size: 12px;
  color: #a0a4ab;
  margin: 8px 0 0;
}

.form-tip {
  font-size: 12px;
  color: #a0a4ab;
  line-height: 1.5;
}

.text-warn {
  color: #e6a23c;
  margin-right: 8px;
}

.scope-desc {
  font-size: 13px;
  color: #606266;
  margin: 0 0 14px;
}

.scope-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: flex-start;
}

.scope-radio {
  height: auto;
  align-items: flex-start;
  padding: 8px 0;
}

.scope-label {
  font-size: 14px;
  color: #303133;
}

.scope-sub {
  font-size: 12px;
  color: #a0a4ab;
  margin-top: 2px;
}
</style>
