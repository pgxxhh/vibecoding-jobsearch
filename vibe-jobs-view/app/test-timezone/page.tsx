'use client';

import { useState, useEffect } from 'react';
import { TimeDisplay, RelativeTime, DateOnly, CompactDateTime } from '@/components/TimeDisplay';
import { SearchForm } from '@/components/SearchForm';
import { TimeUtils } from '@/lib/time-utils';

/**
 * 时区处理测试页面
 * 用于验证时区转换功能是否正常工作
 */
export default function TimeZoneTestPage() {
  const [backendTime, setBackendTime] = useState<any>(null);
  const [searchResults, setSearchResults] = useState<any>(null);

  // 测试用的UTC时间
  const testTimes = [
    "2024-01-15T02:30:00Z",      // UTC 2:30 → CST 10:30
    "2024-01-15T14:45:00Z",      // UTC 14:45 → CST 22:45
    "2024-06-15T10:00:00Z",      // UTC 10:00 → CST 18:00 (夏天)
    "2024-12-01T16:30:00Z",      // UTC 16:30 → CST 00:30+1 (冬天)
    new Date().toISOString(),    // 当前时间
  ];

  // 获取后端时区信息
  useEffect(() => {
    fetch('/api/debug/timezone')
      .then(res => res.json())
      .then(data => setBackendTime(data))
      .catch(err => console.error('Failed to fetch backend time:', err));
  }, []);

  const handleSearch = (params: any) => {
    setSearchResults(params);
    console.log('Search params (will be sent to API):', params);
  };

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-8">
      <div className="bg-white rounded-lg shadow p-6">
        <h1 className="text-2xl font-bold mb-6">时区处理测试页面</h1>
        
        {/* 后端时区信息 */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold mb-4">后端时区配置</h2>
          {backendTime ? (
            <div className="bg-gray-50 p-4 rounded text-sm font-mono space-y-1">
              <div>系统时区: {backendTime.systemTimezone}</div>
              <div>当前UTC时间: {backendTime.currentUtcTime}</div>
              <div>转换为本地显示: <TimeDisplay utcTime={backendTime.currentUtcTime} /></div>
              <div>相对时间: <RelativeTime utcTime={backendTime.currentUtcTime} /></div>
            </div>
          ) : (
            <div className="text-gray-500">Loading backend info...</div>
          )}
        </div>

        {/* 时间转换测试 */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold mb-4">时间显示测试</h2>
          <div className="overflow-x-auto">
            <table className="w-full border-collapse border border-gray-300">
              <thead>
                <tr className="bg-gray-100">
                  <th className="border border-gray-300 p-2 text-left">UTC时间</th>
                  <th className="border border-gray-300 p-2 text-left">完整时间</th>
                  <th className="border border-gray-300 p-2 text-left">仅日期</th>
                  <th className="border border-gray-300 p-2 text-left">紧凑格式</th>
                  <th className="border border-gray-300 p-2 text-left">相对时间</th>
                </tr>
              </thead>
              <tbody>
                {testTimes.map((utcTime, index) => (
                  <tr key={index}>
                    <td className="border border-gray-300 p-2 font-mono text-xs">
                      {utcTime}
                    </td>
                    <td className="border border-gray-300 p-2">
                      <TimeDisplay utcTime={utcTime} format="DATETIME" />
                    </td>
                    <td className="border border-gray-300 p-2">
                      <DateOnly utcTime={utcTime} />
                    </td>
                    <td className="border border-gray-300 p-2">
                      <CompactDateTime utcTime={utcTime} />
                    </td>
                    <td className="border border-gray-300 p-2">
                      <RelativeTime utcTime={utcTime} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* 搜索时间参数测试 */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold mb-4">搜索时间参数测试</h2>
          <SearchForm onSearch={handleSearch} />
          
          {searchResults && (
            <div className="mt-4">
              <h3 className="font-medium mb-2">生成的API参数:</h3>
              <div className="bg-gray-50 p-4 rounded text-sm font-mono">
                <pre>{JSON.stringify(searchResults, null, 2)}</pre>
              </div>
              
              {searchResults.startTime && searchResults.endTime && (
                <div className="mt-4 p-4 bg-blue-50 rounded">
                  <h4 className="font-medium mb-2">时间范围验证:</h4>
                  <div className="text-sm space-y-1">
                    <div>
                      开始时间: {searchResults.startTime} → 
                      <TimeDisplay utcTime={searchResults.startTime} className="ml-2 font-semibold" />
                    </div>
                    <div>
                      结束时间: {searchResults.endTime} → 
                      <TimeDisplay utcTime={searchResults.endTime} className="ml-2 font-semibold" />
                    </div>
                    <div>
                      范围描述: {TimeUtils.describeSearchRange(searchResults.startTime, searchResults.endTime)}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 工具函数测试 */}
        <div className="mb-8">
          <h2 className="text-lg font-semibold mb-4">工具函数测试</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-gray-50 p-4 rounded">
              <h4 className="font-medium mb-2">时间转换</h4>
              <div className="text-sm space-y-1 font-mono">
                <div>当前UTC: {TimeUtils.nowUTC()}</div>
                <div>当前本地: {TimeUtils.nowLocal()}</div>
                <div>本地日期: {TimeUtils.nowLocal('DATE')}</div>
              </div>
            </div>
            
            <div className="bg-gray-50 p-4 rounded">
              <h4 className="font-medium mb-2">搜索时间转换</h4>
              <div className="text-sm space-y-1 font-mono">
                <div>今天开始: {TimeUtils.toSearchUTC('2024-01-15', false)}</div>
                <div>今天结束: {TimeUtils.toSearchUTC('2024-01-15', true)}</div>
              </div>
            </div>
          </div>
        </div>

        {/* 验证清单 */}
        <div>
          <h2 className="text-lg font-semibold mb-4">验证清单</h2>
          <div className="bg-yellow-50 p-4 rounded">
            <ul className="text-sm space-y-1">
              <li>✅ 后端返回UTC格式时间 (2024-01-01T10:00:00Z)</li>
              <li>✅ 前端显示东八区时间 (2024-01-01 18:00:00)</li>
              <li>✅ 相对时间显示正常 (2小时前)</li>
              <li>✅ 搜索参数转换为UTC</li>
              <li>✅ 时间范围描述正确</li>
              <li>✅ Tooltip显示完整信息</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}