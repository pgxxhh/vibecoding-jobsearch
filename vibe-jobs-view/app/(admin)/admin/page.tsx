import Link from 'next/link';

export default function AdminHomePage() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-gray-900">后台总览</h2>
        <p className="text-sm text-gray-600 mt-1">在这里可以实时调整采集调度、数据源以及公司清单。</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Link
          href="/admin/ingestion-settings"
          className="group rounded-2xl border border-gray-200 bg-white p-6 shadow-sm hover:shadow-md hover:border-brand-300 transition-all"
        >
          <div className="flex items-center gap-3 mb-3">
            <div className="h-10 w-10 rounded-xl bg-brand-100 flex items-center justify-center text-brand-600">
              ⚙️
            </div>
            <h3 className="text-lg font-semibold text-gray-900 group-hover:text-brand-700 transition">采集调度</h3>
          </div>
          <p className="text-sm text-gray-600">修改抓取间隔、并发度以及过滤器配置，几秒内生效。</p>
          <div className="mt-4 text-xs text-brand-600 font-medium">点击管理 →</div>
        </Link>

        <Link
          href="/admin/data-sources"
          className="group rounded-2xl border border-gray-200 bg-white p-6 shadow-sm hover:shadow-md hover:border-brand-300 transition-all"
        >
          <div className="flex items-center gap-3 mb-3">
            <div className="h-10 w-10 rounded-xl bg-brand-100 flex items-center justify-center text-brand-600">
              🔗
            </div>
            <h3 className="text-lg font-semibold text-gray-900 group-hover:text-brand-700 transition">数据源管理</h3>
          </div>
          <p className="text-sm text-gray-600">维护各类数据源、公司列表与运行开关。</p>
          <div className="mt-4 text-xs text-brand-600 font-medium">点击管理 →</div>
        </Link>
      </div>

      <div className="rounded-2xl border border-blue-200 bg-blue-50 p-6">
        <div className="flex items-start gap-3">
          <div className="h-6 w-6 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 text-sm">
            💡
          </div>
          <div>
            <p className="font-medium text-blue-900 mb-2">使用提示</p>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• 所有改动都会记录在审计日志中，方便追溯</li>
              <li>• 调度配置保存后 1-2 秒内自动触发重新调度</li>
              <li>• 数据源保存后会自动刷新客户端缓存，无需重启服务</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
