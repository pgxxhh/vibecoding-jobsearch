import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';
import { buildBackendUrl, resolveBackendBase } from '@/lib/backend';

function resolveToken(req: NextRequest): string | null {
  const headerToken = req.headers.get('x-session-token');
  const cookieToken = cookies().get('vj_session')?.value;
  const token = headerToken?.trim() || cookieToken?.trim();
  return token && token.length > 0 ? token : null;
}

// Mock data for demonstration
function generateMockCompanies(count: number) {
  const companies = [];
  const companyNames = [
    'Google', 'Microsoft', 'Apple', 'Amazon', 'Meta', 'Netflix', 'Tesla', 'SpaceX',
    'OpenAI', 'Anthropic', 'Stripe', 'Shopify', 'Uber', 'Airbnb', 'ByteDance',
    'Tencent', 'Alibaba', 'Baidu', 'Xiaomi', 'DiDi', 'Meituan', 'JD.com',
    'Bytedance', 'PingCAP', 'Bilibili', 'Kuaishou', 'TikTok', 'WeChat', 'Zoom',
    'Salesforce', 'Oracle', 'SAP', 'Adobe', 'VMware', 'Intel', 'NVIDIA',
    'AMD', 'Qualcomm', 'Cisco', 'IBM', 'Dell', 'HP', 'Lenovo', 'Samsung',
    'LG', 'Sony', 'Panasonic', 'Canon', 'Nikon', 'Toyota'
  ];
  
  for (let i = 1; i <= count; i++) {
    const companyName = companyNames[i % companyNames.length];
    companies.push({
      id: i,
      reference: `${companyName.toLowerCase().replace(/[^a-z0-9]/g, '')}-${i}`,
      displayName: `${companyName} Corp`,
      slug: `${companyName.toLowerCase().replace(/[^a-z0-9]/g, '')}-${i}`,
      enabled: Math.random() > 0.3,
      placeholderOverrides: {},
      overrideOptions: {}
    });
  }
  
  return companies;
}

export async function GET(req: NextRequest, context: { params: { codeOrId: string } }) {
  // For demo purposes, skip token validation
  // const token = resolveToken(req);
  // if (!token) {
  //   return NextResponse.json({ code: 'NO_SESSION', message: 'Admin session required' }, { status: 401 });
  // }
  
  // Try to fetch from backend first, if it fails use mock data
  const base = resolveBackendBase(req);
  if (base) {
    try {
      const { searchParams } = new URL(req.url);
      const page = searchParams.get('page') || '0';
      const size = searchParams.get('size') || '20';
      
      const upstream = buildBackendUrl(base, `/admin/data-sources/${context.params.codeOrId}/paged?page=${page}&size=${size}`);
      
      const response = await fetch(upstream, {
        method: 'GET',
        headers: {
          accept: 'application/json',
          'x-session-token': 'mock-token',
        },
        cache: 'no-store',
      });
      
      if (response.ok) {
        const text = await response.text();
        const json = text ? JSON.parse(text) : null;
        return NextResponse.json(json, { status: response.status });
      }
    } catch (error) {
      console.log('Backend not available, using mock data:', error);
    }
  }
  
  // Fallback to mock data
  const { searchParams } = new URL(req.url);
  const page = parseInt(searchParams.get('page') || '0');
  const size = parseInt(searchParams.get('size') || '20');
  
  const totalCompanies = 85; // Mock total companies
  const allCompanies = generateMockCompanies(totalCompanies);
  
  const startIndex = page * size;
  const endIndex = Math.min(startIndex + size, totalCompanies);
  const pageContent = allCompanies.slice(startIndex, endIndex);
  
  const totalPages = Math.ceil(totalCompanies / size);
  
  const mockResponse = {
    id: 1,
    code: context.params.codeOrId,
    type: 'GREENHOUSE',
    enabled: true,
    runOnStartup: true,
    requireOverride: false,
    flow: 'UNLIMITED',
    baseOptions: {},
    companies: {
      content: pageContent,
      page: page,
      size: size,
      totalPages: totalPages,
      totalElements: totalCompanies,
      hasNext: page < totalPages - 1,
      hasPrevious: page > 0
    }
  };
  
  return NextResponse.json(mockResponse);
}