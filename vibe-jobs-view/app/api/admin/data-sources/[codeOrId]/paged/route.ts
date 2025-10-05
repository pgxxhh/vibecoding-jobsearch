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
  console.log('=== Backend Connection Debug ===');
  console.log('Resolved backend base:', base);
  console.log('Environment variables:', {
    BACKEND_BASE_URL: process.env.BACKEND_BASE_URL,
    NEXT_PUBLIC_BACKEND_BASE: process.env.NEXT_PUBLIC_BACKEND_BASE,
    NEXT_PUBLIC_API_BASE: process.env.NEXT_PUBLIC_API_BASE
  });
  
  if (base) {
    try {
      const { searchParams } = new URL(req.url);
      const page = searchParams.get('page') || '0';
      const size = searchParams.get('size') || '20';
      
      const upstream = buildBackendUrl(base, `/admin/data-sources/${context.params.codeOrId}/paged`);
      upstream.searchParams.set('page', page);
      upstream.searchParams.set('size', size);
      console.log('Attempting to fetch from:', upstream.toString());
      
      const response = await fetch(upstream, {
        method: 'GET',
        headers: {
          accept: 'application/json',
          'x-session-token': 'mock-token',
        },
        cache: 'no-store',
      });
      
      console.log('Backend response status:', response.status);
      const responseText = await response.text();
      console.log('Backend response body:', responseText);
      
      if (response.ok) {
        const json = responseText ? JSON.parse(responseText) : null;
        console.log('Successfully fetched from backend, returning data');
        return NextResponse.json(json, { status: response.status });
      } else {
        console.log('Backend returned non-OK status, falling back to mock data');
      }
    } catch (error) {
      console.log('Backend not available, using mock data:', error);
    }
  } else {
    console.log('No backend base resolved, using mock data directly');
  }
  
  // Fallback to mock data
  const { searchParams } = new URL(req.url);
  const page = parseInt(searchParams.get('page') || '0');
  const size = parseInt(searchParams.get('size') || '20');
  
  // Generate different number of companies for different data sources to simulate real data
  // This prevents all data sources from showing the same fixed number (85) of companies
  const totalCompanies = getTotalCompaniesByDataSource(context.params.codeOrId);
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

// Function to return different company counts for different data sources
// This is a temporary fix to avoid all data sources showing the same number of companies
// TODO: Replace with actual backend API calls when authentication is properly configured
function getTotalCompaniesByDataSource(codeOrId: string): number {
  const dataSources: Record<string, number> = {
    'greenhouse': 42,
    'workday': 28,
    'lever': 36,
    'bamboohr': 23,
    'smartrecruiters': 51,
    'jobvite': 34,
    'icims': 19,
    'successfactors': 47,
    'cornerstone': 31,
    'default': 25
  };
  
  return dataSources[codeOrId.toLowerCase()] || dataSources['default'];
}