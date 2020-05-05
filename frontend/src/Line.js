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
        seriesField: "type"
    };

    return (
        <AntLine {...config} />
    );
}

export default Line;