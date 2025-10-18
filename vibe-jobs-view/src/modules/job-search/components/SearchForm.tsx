import { useState, useCallback } from 'react';
import { TimeUtils } from '@/shared/lib/time-utils';

interface SearchParams {
  keyword?: string;
  startTime?: string;  // UTC格式
  endTime?: string;    // UTC格式
}

interface SearchFormProps {
  onSearch: (params: SearchParams) => void;
  loading?: boolean;
}

/**
 * 搜索表单组件
 * 
 * 演示如何处理用户输入的时间范围，并转换为UTC格式供API使用
 */
export function SearchForm({ onSearch, loading = false }: SearchFormProps) {
  const [keyword, setKeyword] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [error, setError] = useState('');
  
  const handleSearch = useCallback(() => {
    try {
      setError('');
      
      const params: SearchParams = {};
      
      // 添加关键词
      if (keyword.trim()) {
        params.keyword = keyword.trim();
      }
      
      // 处理时间范围
      if (startDate || endDate) {
        // 如果只有一个日期，使用默认范围
        const start = startDate || '2020-01-01';
        const end = endDate || TimeUtils.nowLocal('DATE');
        
        const timeRange = TimeUtils.createSearchTimeRange(start, end);
        params.startTime = timeRange.startTime;
        params.endTime = timeRange.endTime;
      }
      
      onSearch(params);
      
    } catch (err) {
      setError('日期格式错误，请重新输入');
    }
  }, [keyword, startDate, endDate, onSearch]);
  
  const handleReset = useCallback(() => {
    setKeyword('');
    setStartDate('');
    setEndDate('');
    setError('');
    onSearch({});
  }, [onSearch]);
  
  // 生成时间范围描述
  const timeRangeDescription = (() => {
    if (!startDate && !endDate) return null;
    
    try {
      const start = startDate || '2020-01-01';
      const end = endDate || TimeUtils.nowLocal('DATE');
      const timeRange = TimeUtils.createSearchTimeRange(start, end);
      return TimeUtils.describeSearchRange(timeRange.startTime, timeRange.endTime);
    } catch {
      return '日期格式错误';
    }
  })();
  
  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border">
      <div className="space-y-4">
        {/* 搜索框 */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            搜索关键词
          </label>
          <input
            type="text"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            placeholder="输入职位标题、公司名称等"
            value={keyword}
            onChange={e => setKeyword(e.target.value)}
          />
        </div>
        
        {/* 时间范围 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              开始日期
            </label>
            <input
              type="date"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={startDate}
              onChange={e => setStartDate(e.target.value)}
            />
          </div>
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              结束日期
            </label>
            <input
              type="date"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              value={endDate}
              onChange={e => setEndDate(e.target.value)}
            />
          </div>
        </div>
        
        {/* 时间范围预览 */}
        {timeRangeDescription && (
          <div className="text-sm text-gray-600 bg-gray-50 p-3 rounded-md">
            <span className="font-medium">搜索时间范围：</span>
            {timeRangeDescription}
          </div>
        )}
        
        {/* 错误信息 */}
        {error && (
          <div className="text-sm text-red-600 bg-red-50 p-3 rounded-md">
            {error}
          </div>
        )}
        
        {/* 操作按钮 */}
        <div className="flex gap-3">
          <button
            type="button"
            onClick={handleSearch}
            disabled={loading}
            className="flex-1 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? '搜索中...' : '搜索'}
          </button>
          
          <button
            type="button"
            onClick={handleReset}
            disabled={loading}
            className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            重置
          </button>
        </div>
      </div>
    </div>
  );
}

/**
 * 使用示例：
 * 
 * function JobSearchPage() {
 *   const [results, setResults] = useState([]);
 *   const [loading, setLoading] = useState(false);
 *   
 *   const handleSearch = async (params: SearchParams) => {
 *     setLoading(true);
 *     try {
 *       const response = await fetch('/api/jobs/search', {
 *         method: 'POST',
 *         headers: { 'Content-Type': 'application/json' },
 *         body: JSON.stringify(params)  // params中的时间已经是UTC格式
 *       });
 *       const data = await response.json();
 *       setResults(data.jobs);
 *     } finally {
 *       setLoading(false);
 *     }
 *   };
 *   
 *   return (
 *     <div>
 *       <SearchForm onSearch={handleSearch} loading={loading} />
 *       <JobResultList results={results} />
 *     </div>
 *   );
 * }
 */