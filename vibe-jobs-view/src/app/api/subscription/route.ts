import { NextResponse } from 'next/server';

export async function POST(req: Request) {
  const body = await req.json();
  // TODO: 实际存储订阅逻辑，可接入数据库或推送服务
  // 这里只做演示，直接返回成功
  return NextResponse.json({ success: true, message: '订阅已创建', params: body });
}

