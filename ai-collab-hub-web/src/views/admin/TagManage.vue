<template>
  <div class="tag-manage-page">
    <el-card shadow="never" class="main-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-title">标签管理</span>
            <span class="card-sub">填写内容时用户从这里选标签，标签太乱会影响分类效果</span>
          </div>
        </div>
      </template>

      <div class="add-row">
        <el-input
          v-model="newTagName"
          placeholder="输入新标签名称，回车添加"
          class="add-input"
          maxlength="20"
          show-word-limit
          @keyup.enter="handleAdd"
        />
        <el-button type="primary" :icon="Plus" :loading="adding" @click="handleAdd">添加标签</el-button>
      </div>

      <div class="tag-stats">
        当前共 <b>{{ list.length }}</b> 个标签
      </div>

      <div v-loading="loading" class="tag-wrap">
        <template v-if="list.length">
          <el-tag
            v-for="t in list"
            :key="t.id"
            class="tag-item"
            closable
            size="large"
            @close="handleDelete(t)"
          >
            {{ t.tagName }}
          </el-tag>
        </template>
        <el-empty v-else-if="!loading" description="还没有标签，先添加几个吧" :image-size="90" />
      </div>
    </el-card>
  </div>
</template>

<script setup>
/**
 * 标签管理页。
 * 后台标签库是填写内容时的可选项，管理员在这里增删。
 */
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getArticleTagList } from '@/api/blog'
import { addTag, deleteTag } from '@/api/admin'

const loading = ref(false)
const adding = ref(false)
const list = ref([])
const newTagName = ref('')

async function loadList() {
  loading.value = true
  try {
    const res = await getArticleTagList()
    list.value = res?.data || []
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  const name = newTagName.value.trim()
  if (!name) {
    ElMessage.warning('请输入标签名称')
    return
  }
  if (list.value.some((t) => t.tagName === name)) {
    ElMessage.warning('这个标签已经存在了')
    return
  }
  adding.value = true
  try {
    await addTag({ tagName: name })
    ElMessage.success('标签已添加')
    newTagName.value = ''
    loadList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    adding.value = false
  }
}

async function handleDelete(tag) {
  try {
    await ElMessageBox.confirm(
        `确定要删除标签「${tag.tagName}」吗？已经使用了这个标签的内容不受影响。`,
        '删除确认',
        { type: 'warning' }
    )
    await deleteTag(tag.id)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    // 取消
  }
}

onMounted(loadList)
</script>

<style scoped>
.main-card {
  border-radius: 10px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d3a6b;
  margin-right: 12px;
}

.card-sub {
  font-size: 12px;
  color: #909399;
}

.add-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.add-input {
  max-width: 320px;
}

.tag-stats {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

.tag-stats b {
  color: #1d3a6b;
  font-size: 15px;
}

.tag-wrap {
  min-height: 160px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-content: flex-start;
  padding: 16px;
  background: #f8f9fb;
  border-radius: 10px;
}

.tag-item {
  font-size: 14px;
}

.tag-wrap :deep(.el-empty) {
  margin: 0 auto;
}
</style>
