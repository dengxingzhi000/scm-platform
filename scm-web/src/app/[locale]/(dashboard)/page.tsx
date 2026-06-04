"use client";

import { useTranslations } from "next-intl";
import { Card, Col, Row, Statistic, Table, Typography } from "antd";
import {
  ShoppingCartOutlined,
  CarryOutOutlined,
  WarningOutlined,
  DollarOutlined,
} from "@ant-design/icons";

const recentOrders = [
  { key: "1", orderNo: "ORD-20260604-001", status: "已完成", amount: 12500 },
  { key: "2", orderNo: "ORD-20260604-002", status: "待发货", amount: 8300 },
  { key: "3", orderNo: "ORD-20260604-003", status: "处理中", amount: 25600 },
  { key: "4", orderNo: "ORD-20260604-004", status: "待审核", amount: 18200 },
];

const columns = [
  { title: "订单号", dataIndex: "orderNo", key: "orderNo" },
  { title: "状态", dataIndex: "status", key: "status" },
  { title: "金额", dataIndex: "amount", key: "amount" },
];

export default function DashboardPage() {
  const t = useTranslations();

  return (
    <div style={{ padding: 24 }}>
      <Typography.Title level={4}>
        {t("dashboard.welcome")}, SCM Platform
      </Typography.Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t("dashboard.stats.todayOrders")}
              value={128}
              prefix={<ShoppingCartOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t("dashboard.stats.pendingShipments")}
              value={36}
              prefix={<CarryOutOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t("dashboard.stats.lowStock")}
              value={12}
              prefix={<WarningOutlined />}
              valueStyle={{ color: "#cf1322" }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t("dashboard.stats.monthlyRevenue")}
              value={486500}
              prefix={<DollarOutlined />}
            />
          </Card>
        </Col>
      </Row>
      <Card title={t("dashboard.recentOrders")} style={{ marginTop: 24 }}>
        <Table dataSource={recentOrders} columns={columns} pagination={false} />
      </Card>
    </div>
  );
}
