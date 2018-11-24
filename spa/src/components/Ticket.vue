<template>
  <div class="ticket">
    
      <!-- <button @click.prevent="to_finish()">go test</button> -->
      <div class="ti" v-for="t in tickets">
        <div style="min-width:6em;">{{t.name}}</div>
        <div>{{t.price}}(元)</div>
        <div class="count">
          {{t.count}}(张)
          &nbsp;
          <button class="op" @click.prevent="++t.count">&plus;</button>
          &nbsp;
          <button class="op" @click.prevent="t.count > 0 ? --t.count: 0" v-bind:disabled="t.count <= 0">&minus;</button>
        </div>
      </div>
      <div style="color:white;margin-top:.5em;">总计：{{parseFloat(total).toFixed(2)}}(元)</div>
      <button v-if="total>0 && !requesting" @click.prevent="gzh_buy()">公众号购买</button>
      <button class="scan" v-if="total>0 && !requesting" @click.prevent="qr_buy()">扫码支付</button>
      <div v-if="requesting" style="color:gold;"> <h2>请稍后……</h2> </div>
      <canvas id="qr"></canvas>
    
  </div>
</template>

<script>
import _ from "lodash";
import moment from "moment";
import QRious from "qrious";
import ws from "../ws";
export default {
  name: "Ticket",
  props: {
    msg: String
  },
  created: function() {
    this.$root.$on("req_qr", data => {
      this.requesting = false
      // this.save_session(data.data)
      this.cb(data);
    });
    this.$root.$on("pay_success", data => {      
      this.$router.push({ path: 'finish', query: data.data})
      // this.$router.replace('finish')
      
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
    },
    caption() {
      let title = _.reduce(
        this.tickets,
        (cap, t) => {
          return t.count > 0 ?cap + `${t.name}(${t.count})+` : cap;
        },
        ''
      );
      if(title.length > 1) title = title.slice(0, title.length - 1)
      return title;
    }
  },
  methods: {
    // to_finish(){
    //   this.$router.replace('finish')
    // },
    // save_session(order){
    //   console.log('save session:'+ order, typeof order)
    //   sessionStorage.setItem('order', order) 
    // },
    req_qr() {
      this.requesting = true;
      const data = {
        cmd: "req_qr",
        data: JSON.stringify({
          out_trade_no: moment().format("YYYYMMDDHHmmssSSS"),
          total_amount: this.total * 100,
          body: this.caption,
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
  border: 2px inset grey;
  margin: 0.5em .5rem 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: nowrap;
}
.ticket {
  display: flex;
  /* justify-content: space-between; */
  flex-flow: column;
  overflow: hidden;
  font-weight: 700;
}

button {
  font-size: 1.2rem;
  margin: 0.5em .5rem;
  background-color: bisque;
  border-radius: 0.9em;
  color: #42b983;
}
button:disabled{
  background-color: #CCC;
  color: lightslategrey;
}
.scan{
  background-color: saddlebrown;
}
.op {
  opacity: .7;
  background-color: lightsalmon;
  color: blue;
  padding: 0.2em .5em;
  font-weight: 900;
  display: inline-block;
  margin: inherit;
}

</style>
