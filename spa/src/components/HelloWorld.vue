<template>
  <div class="hello">
    <header>南岳门票处</header>
    <div class="content">
      <div class="ti" v-for="t in tickets">
        <div style="min-width:7em;">{{t.name}}</div>
        <div>{{t.price}}(元)</div>
        <div class="count">
          {{t.count}}(张)
          &nbsp;
          <button class="op" @click.prevent="++t.count">+</button>
          &nbsp;
          <button class="op" @click.prevent="t.count > 0 ? --t.count: 0">-</button>
        </div>
      </div>
      <div style="color:white;margin-top:.5em;">总计：{{parseFloat(total).toFixed(2)}}(元)</div>
      <button v-if="total>0 && !requesting" @click.prevent="gzh_buy()">公众号购买</button>
      <button v-if="total>0 && !requesting" @click.prevent="qr_buy()">扫码支付</button>
      <div v-if="requesting" style="color:gold;"> 请稍后……</div>
      <canvas id="qr"></canvas>
    </div>
    <footer>for 建行支付测试</footer>
  </div>
</template>

<script>
import _ from "lodash";
import moment from "moment";
import QRious from "qrious";
import ws from "../ws";
export default {
  name: "HelloWorld",
  props: {
    msg: String
  },
  created: function() {
    this.$root.$on("req_qr", data => {
      this.requesting = false
      this.cb(data);
    });
    this.$root.$on("pay_success", data => {
      alert('支付成功')
    });
    
  },
  mounted() {},
  data() {
    return {
      requesting: false,
      cb: null,
      tickets: [
        {
          name: "中心景区门票",
          price: "0.01",
          count: 0
        },
        {
          name: "大庙门票",
          price: "0.02",
          count: 0
        },
        {
          name: "水帘洞门票",
          price: "0.03",
          count: 0
        },
        {
          name: "车锁全程票",
          price: "0.04",
          count: 0
        }
      ]
    };
  },
  computed: {
    total() {
      let total = _.reduce(
        this.tickets,
        (sum, t) => {
          return sum + t.price * t.count;
        },
        0
      );
      return total;
    }
  },
  methods: {
    req_qr() {
      this.requesting = true;
      const data = {
        cmd: "req_qr",
        data: JSON.stringify({
          out_trade_no: moment().format("YYYYMMDDHHmmssSSS"),
          total_amount: this.total * 100,
          body: "南岳门票",
          cli_id
        })
      };
      console.log('req data = ', data)
      ws.send(data);
    },
    gzh_buy() {
      this.cb = data => {
        if (data.ret == 0) {
          location.href = data.qr_url;
        } else {
          alert(`请求支付失败：${data.msg}`)
        }
      };
      this.req_qr();
    },
    qr_buy() {
      this.cb = data => {
        if (data.ret == 0) {
          const qr = new QRious({
            size: 200,
            background: "#fff",
            foreground: "#284a9f",
            element: document.getElementById('qr'),
            value: data.qr_url
          });
        } else {
          alert(`请求支付失败：${data.msg}`)
        }
      };
      this.req_qr();
    }
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
canvas{
  margin: 0.3em auto;
  width: 200px;
  /* height: 200px; */
}
.count {
  display: flex;
  align-items: center;
}
.ti {
  border: 1px dashed purple;
  margin: 0.5em 1rem 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: nowrap;
}
.hello {
  display: flex;
  /* justify-content: space-between; */
  flex-flow: column;
  overflow: hidden;
  font-weight: 700;
}
header {
  background-color: rgb(132, 226, 69);
}
footer {
  /* bottom: 0;
  position: fixed; */
  background-color: lightgray;
}
header,
footer {
  text-align: center;
  min-width: 100%;
  min-height: 2rem;
  line-height: 2em;
}
button {
  font-size: 1.2rem;
  margin: 0.5em 1rem;
  background-color: bisque;
  border-radius: 0.7em;
  color: #42b983;
}
.op {
  font-weight: 900;
  display: inline-block;
  margin: inherit;
}
.content {
  display: flex;
  flex-flow: column;
  background-image: url("../assets/bg.jpg");
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  min-height: calc(100vh - 4rem);
}
</style>
