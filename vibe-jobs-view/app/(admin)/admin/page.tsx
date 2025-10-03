import Link from 'next/link';

export default function AdminHomePage() {
  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-semibold text-white">后台总览</h2>
        <p className="text-sm text-white/70">在这里可以实时调整采集调度、数据源以及公司清单。</p>
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <Link
          href="/admin/ingestion-settings"
          className="rounded-xl border border-white/10 bg-white/5 p-4 hover:border-white/30 hover:bg-white/10"
        >
          <h3 className="text-lg font-semibold text-white">采集调度</h3>
          <p className="text-sm text-white/70">修改抓取间隔、并发度以及过滤器配置，几秒内生效。</p>
        </Link>
        <Link
          href="/admin/data-sources"
          className="rounded-xl border border-white/10 bg-white/5 p-4 hover:border-white/30 hover:bg-white/10"
        >
          <h3 className="text-lg font-semibold text-white">数据源管理</h3>
          <p className="text-sm text-white/70">维护各类数据源、公司列表与运行开关。</p>
        </Link>
      </div>
      <div className="rounded-xl border border-white/10 bg-white/5 p-4 text-sm text-white/60">
        <p className="font-medium text-white">提示</p>
        <ul className="mt-2 list-disc space-y-1 pl-5">
          <li>所有改动都会记录在审计日志中，方便追溯。</li>
          <li>调度配置保存后 1-2 秒内自动触发重新调度。</li>
          <li>数据源保存后会自动刷新客户端缓存，无需重启服务。</li>
        </ul>
      </div>
    </div>
  );
}
