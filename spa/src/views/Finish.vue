<template>
  <div class="finish">
    <header>南岳门票处</header>
    <div class="content">
      <div class="ti">
        <div><h2>恭喜购票成功</h2></div>
        <div>
          <div class="caption">订单号：</div> 
          <div>{{order.out_trade_no}}</div>
        </div>
        <div>
          <div class="caption">名称：</div> 
          <div>{{order.body}}</div>
        </div>
        <div>
          <div class="caption">价格：</div> 
          <div>{{parseFloat(order.total_amount/100).toFixed(2)}}(元)</div>
        </div>
      </div>
      <router-link class="gohome" to="/">再次购买</router-link>
    </div>
    <footer>for 建行支付测试</footer>
  </div>
</template>

<script>
import _ from "lodash";
import moment from "moment";
import QRious from "qrious";
export default {
  name: "Finish",
  props: {
    msg: String
  },
  created: function() {

  },
  mounted() {
    this.order = JSON.parse(sessionStorage.getItem('order'))
  },
  data() {
    return {
      order: {},
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
    save_session(order){
      console.log('save session:'+JSON.stringify(order))
      sessionStorage.setItem('order', JSON.stringify(order) )
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
  opacity: .7;
  padding: .5em;
  /* background-color: grey; */
  margin: 1.5em .5rem 0;
  display: flex;
  align-items: center;
  flex-flow: column;
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
header {
  background-color: rgb(132, 226, 69);
}
footer {
  /* bottom: 0;
  position: fixed; */
  background-color: lightgray;
}
.ti > div{
  color: white;
  margin-top: 1em;
  display: flex;
  flex-wrap: nowrap;
}
.ti > div:last-child{
  margin-bottom: 1em;
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
  margin: 0.5em .5rem;
  background-color: bisque;
  border-radius: 0.9em;
  color: #42b983;
}
.caption{
  min-width: 4em;
}
.content {
  display: flex;
  flex-flow: column;
  align-items: center;
  background-image: url("../assets/bg.jpg");
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  min-height: calc(100vh - 4rem);
}
.gohome{
  /* width: 8em; */
  padding: .8em;
  border-radius: 0.9em;
  display: block;
  background-color: #42b983;
  margin-top: 3em;
}
</style>
