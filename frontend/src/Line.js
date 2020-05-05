import React from 'react';
import {Line as AntLine} from '@ant-design/charts';


const Line = function (props) {

    const config = {
        data: props.data,
        xField: 'dateTime',
        yField: 'value',
        forceFit: true,
        padding: 'auto',
        point: {
            visible: true,
        },
        seriesField: "type",
        interactions: [
            {
                type: 'slider',
                cfg: {
                    start: 0,
                    end: 1,
                },
            },
        ]
    };

    return (
        <AntLine {...config} />
    );
}

export default Line;