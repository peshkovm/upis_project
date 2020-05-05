import React from 'react';
import './App.css';
import {Card, Layout, List, Space, Tabs, Typography} from 'antd';
import {
    CheckCircleTwoTone,
    ClockCircleOutlined,
    CloseSquareTwoTone,
    FacebookFilled,
    GoogleSquareFilled,
    WarningTwoTone
} from "@ant-design/icons";
import Line from "./Line";
import {SSEProvider, useSSE} from 'react-hooks-sse';

const {Content} = Layout;
const {Text} = Typography;
const {TabPane} = Tabs;

const SentimentIcon = function (sentiment) {
    let icon;
    let color;

    switch (sentiment) {
        case "positive":
            color = "#52c41a";
            icon = <CheckCircleTwoTone twoToneColor={color} style={{fontSize: "16px"}}/>;
            break;
        case "neutral":
            color = "#fadb14";
            icon = <WarningTwoTone twoToneColor={color} style={{fontSize: "16px"}}/>
            break;
        case "negative":
            color = "#f5222d";
            icon = <CloseSquareTwoTone twoToneColor={color} style={{fontSize: "16px"}}/>
            break;
        default:
    }

    return <Space>
        {icon}
        <Text strong style={{color: color}}>{sentiment}</Text>
    </Space>;
}

const DateIcon = (date) => (
    <Space>
        <ClockCircleOutlined style={{fontSize: "16px"}}/>
        {date}
    </Space>
);

const News = () => {
    const data = useSSE('news', [], {
        stateReducer(state, action) {
            return [...state, action.data];
        }
    });

    return (
        <Card title="Новости" hoverable={true} bordered={false} style={{marginTop: "24px"}}>
            <List
                itemLayout="vertical"
                dataSource={data}
                pagination={{
                    pageSize: 3,
                    position: "both"
                }}
                renderItem={item => (
                    <List.Item
                        key={item.title}
                        onClick={() => console.log("click " + item.title)}
                        actions={[
                            SentimentIcon(item.sentiment),
                            DateIcon(item.dateTime),
                            CompanyIcon(item.company)
                        ]}
                    >
                        <List.Item.Meta title={item.title} description={item.description}/>
                    </List.Item>
                )}
            />
        </Card>);
}

const Chart = () => {
    const chartDataGoogle = useSSE('Google', [], {
        stateReducer(state, action) {
            console.log([action.data]);
            return [...state, action.data];
        }
    });

    const chartDataFacebook = useSSE('Facebook', [], {
        stateReducer(state, action) {
            return [...state, action.data];
        }
    });

    return <Card title="График" hoverable={true} bordered={false}>
        <Tabs defaultActiveKey="1">
            <TabPane tab="Google" key="1" forceRender={true}>
                <Line data={chartDataGoogle}/>
            </TabPane>
            <TabPane tab="Facebook" key="2" forceRender={true}>
                <Line data={chartDataFacebook}/>
            </TabPane>
        </Tabs>
    </Card>
}

const CompanyIcon = function (company) {
    let icon;

    switch (company) {
        case "Google":
            icon = <GoogleSquareFilled style={{fontSize: "16px"}}/>
            break;
        case "Facebook":
            icon = <FacebookFilled style={{fontSize: "16px"}}/>
            break;
        default:
    }
    return <Space>
        {icon}
        {company}
    </Space>;
}

const App = function () {
    return (
        <Layout className="layout">
            <Content style={{margin: '24px 24px'}}>
                <SSEProvider endpoint="http://localhost:8080/api/chartData">
                    <Chart/>
                </SSEProvider>
                <SSEProvider endpoint="http://localhost:8080/api/news">
                    <News/>
                </SSEProvider>
            </Content>
        </Layout>
    );
};

export default App;