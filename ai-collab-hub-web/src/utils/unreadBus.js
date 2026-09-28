import { reactive } from 'vue'

/**
 * 全局未读状态（红点专用）。
 *
 * 【为什么要这个】
 * 顶部导航的「消息中心」「站内沟通」两个红点原来只靠 60 秒轮询刷新 ——
 * 用户点了「全部已读」或者进会话读完消息，红点要等最多一分钟才消失，看着像没生效。
 *
 * 这里用一个全局响应式状态当"信号灯"：
 *   任何地方做完已读操作 -> 调 refreshUnread() -> 版本号 +1
 *   导航栏 watch 版本号 -> 立刻重新拉未读数
 *
 * 【为什么不装 mitt 之类的库】
 * 需求就这么点：一个计数器 + 一次广播。用 vue 自带的 reactive 就够了，
 * 少一个依赖、少一份体积。
 *
 * 【用法】
 *   操作完已读后：  import { refreshUnread } from '@/utils/unreadBus'
 *                  refreshUnread()
 *   需要监听的地方：watch(() => unreadBus.version, () => { ...重新拉数据... })
 */
export const unreadBus = reactive({
  /** 版本号。每次有人操作完已读就 +1，监听方据此重新拉数据 */
  version: 0,
  /** 最近一次广播的原因，方便调试时看是谁触发的 */
  lastReason: ''
})

/**
 * 广播「未读状态可能变了」。
 *
 * @param {string} reason 触发原因（调试用，比如 'chat-read' / 'notice-read-all'）
 */
export function refreshUnread(reason = '') {
  unreadBus.version += 1
  unreadBus.lastReason = reason
}
