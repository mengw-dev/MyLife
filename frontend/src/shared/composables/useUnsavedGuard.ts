/**
 * 未保存修改守卫（DEV-NOTES §5）：路由离开前确认。
 */
import { onBeforeRouteLeave } from 'vue-router'
import { ElMessageBox } from 'element-plus'

export function useUnsavedGuard(isDirty: () => boolean, message = '当前有未保存的修改，确定离开吗？') {
  onBeforeRouteLeave(async () => {
    if (!isDirty()) return true
    try {
      await ElMessageBox.confirm(message, '未保存的修改', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '留下',
      })
      return true
    } catch {
      return false
    }
  })
}