<template>
  <div id="app">
    <header>南岳门票处</header>
    <div class="content">
      <router-view/>
    </div>
    <footer>for 建行支付测试</footer>
  </div>
</template>

<style>
:root {
  box-sizing: border-box;
  font-size: calc(1vw + 0.9em);
}
body {
  margin: 0;
  font-family: Helvetica, Arial, sans-serif;
}
#app {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
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
.content {
  display: flex;
  flex-flow: column;
  background-image: url("./assets/bg.jpg");
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
  min-height: calc(100vh - 4rem);
}
</style>
