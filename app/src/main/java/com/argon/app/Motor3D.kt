package com.argon.app

// Motor 3D mínimo (Canvas 2D, sin internet). Se inyecta solo cuando el HTML usa Argon3D.
object Motor3D {
    const val JS = """(function () {
  var W = window;
  var colC = {}, sprC = {};

  function parse(c) {
    if (Array.isArray(c)) return c;
    c = String(c);
    if (colC[c]) return colC[c];
    var k = document.createElement('canvas');
    k.width = k.height = 1;
    var x = k.getContext('2d');
    x.fillStyle = '#c8c8c8';
    x.fillStyle = c;
    var s = String(x.fillStyle), r;
    if (s.charAt(0) === '#') {
      if (s.length === 4) s = '#' + s[1] + s[1] + s[2] + s[2] + s[3] + s[3];
      r = [parseInt(s.substr(1, 2), 16), parseInt(s.substr(3, 2), 16), parseInt(s.substr(5, 2), 16)];
    } else {
      var m = s.match(/[\d.]+/g) || [200, 200, 200];
      r = [+m[0], +m[1], +m[2]];
    }
    colC[c] = r;
    return r;
  }

  function mix(a, b, t) {
    return [a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
  }

  function css(a) {
    return 'rgb(' + Math.round(a[0]) + ',' + Math.round(a[1]) + ',' + Math.round(a[2]) + ')';
  }

  function sprite(rgb) {
    var q = [Math.round(rgb[0] / 12) * 12, Math.round(rgb[1] / 12) * 12, Math.round(rgb[2] / 12) * 12];
    var key = q.join(',');
    if (sprC[key]) return sprC[key];
    var S = 96, h = S / 2, k = document.createElement('canvas');
    k.width = k.height = S;
    var x = k.getContext('2d');
    var g = x.createRadialGradient(h * 0.65, h * 0.6, 0, h, h, h);
    g.addColorStop(0, css(mix(q, [255, 255, 255], 0.75)));
    g.addColorStop(0.35, css(mix(q, [255, 255, 255], 0.15)));
    g.addColorStop(0.75, css(q));
    g.addColorStop(1, css(mix(q, [0, 0, 0], 0.6)));
    x.fillStyle = g;
    x.beginPath();
    x.arc(h, h, h - 1, 0, 6.2832);
    x.fill();
    sprC[key] = k;
    return k;
  }

  function escena(o) {
    o = o || {};
    var S = {
      fondo: o.fondo || '#000',
      rotar: o.rotar !== false,
      vel: o.velocidad === undefined ? 0.5 : o.velocidad,
      yaw: o.yaw === undefined ? 0.6 : o.yaw,
      pitch: o.pitch === undefined ? 0.35 : o.pitch,
      zoom: o.zoom || 1
    };
    var esf = [], pol = [], lin = [], txt = [];

    S.esfera = function (x, y, z, r, c) {
      esf.push({ p: [x, y, z], r: r, s: sprite(parse(c || '#6c9bff')) });
      return S;
    };

    S.malla = function (V, F, c) {
      var rgb = parse(c || '#6c9bff');
      F.forEach(function (f) {
        pol.push({ v: f.map(function (i) { return V[i]; }), c: rgb });
      });
      return S;
    };

    S.caja = function (x, y, z, w, h, d, c) {
      var a = w / 2, b = h / 2, e = d / 2;
      var P = [[-a, -b, -e], [a, -b, -e], [a, b, -e], [-a, b, -e], [-a, -b, e], [a, -b, e], [a, b, e], [-a, b, e]]
        .map(function (q) { return [q[0] + x, q[1] + y, q[2] + z]; });
      return S.malla(P, [[0, 1, 2, 3], [4, 5, 6, 7], [0, 1, 5, 4], [2, 3, 7, 6], [1, 2, 6, 5], [0, 3, 7, 4]], c);
    };

    S.cilindro = function (x, y, z, r, h, c, n) {
      n = n || 24;
      var V = [], F = [], top = [], bot = [], i, j, t;
      for (i = 0; i < n; i++) {
        t = i / n * 6.2832;
        V.push([x + r * Math.cos(t), y - h / 2, z + r * Math.sin(t)]);
        V.push([x + r * Math.cos(t), y + h / 2, z + r * Math.sin(t)]);
      }
      for (i = 0; i < n; i++) {
        j = (i + 1) % n;
        F.push([2 * i, 2 * i + 1, 2 * j + 1, 2 * j]);
        top.push(2 * i + 1);
        bot.push(2 * i);
      }
      F.push(top);
      F.push(bot);
      return S.malla(V, F, c);
    };

    S.superficie = function (f, u0, u1, v0, v1, nu, nv, c) {
      nu = nu || 30;
      nv = nv || 30;
      var i, j, G = [];
      for (i = 0; i <= nu; i++) {
        G[i] = [];
        for (j = 0; j <= nv; j++) G[i][j] = f(u0 + (u1 - u0) * i / nu, v0 + (v1 - v0) * j / nv);
      }
      for (i = 0; i < nu; i++) {
        for (j = 0; j < nv; j++) {
          var uc = u0 + (u1 - u0) * (i + 0.5) / nu, vc = v0 + (v1 - v0) * (j + 0.5) / nv;
          pol.push({
            v: [G[i][j], G[i + 1][j], G[i + 1][j + 1], G[i][j + 1]],
            c: parse(typeof c === 'function' ? c(uc, vc) : (c || '#6c9bff'))
          });
        }
      }
      return S;
    };

    S.toro = function (x, y, z, R, r, c, n, m) {
      return S.superficie(function (u, v) {
        return [x + (R + r * Math.cos(v)) * Math.cos(u), y + r * Math.sin(v), z + (R + r * Math.cos(v)) * Math.sin(u)];
      }, 0, 6.2832, 0, 6.2832, n || 32, m || 14, c);
    };

    S.linea = function (a, b, c, g) {
      lin.push({ a: a, b: b, c: css(parse(c || '#ffffff')), w: g || 2 });
      return S;
    };

    S.texto = function (x, y, z, s, c, tam) {
      txt.push({ p: [x, y, z], t: String(s), c: css(parse(c || '#ffffff')), sz: tam || 14 });
      return S;
    };

    function cr(p0, p1, p2, p3, t) {
      var t2 = t * t, t3 = t2 * t, o = [], q;
      for (q = 0; q < 3; q++) {
        o.push(0.5 * (2 * p1[q] + (-p0[q] + p2[q]) * t + (2 * p0[q] - 5 * p1[q] + 4 * p2[q] - p3[q]) * t2 +
          (-p0[q] + 3 * p1[q] - 3 * p2[q] + p3[q]) * t3));
      }
      return o;
    }

    S.tubo = function (P, r, c, paso) {
      var n = P.length, pts = [], i, k;
      if (!n) return S;
      for (i = 0; i < n - 1; i++) {
        var p0 = P[Math.max(0, i - 1)], p1 = P[i], p2 = P[i + 1], p3 = P[Math.min(n - 1, i + 2)];
        var L = Math.sqrt(Math.pow(p2[0] - p1[0], 2) + Math.pow(p2[1] - p1[1], 2) + Math.pow(p2[2] - p1[2], 2));
        var m = Math.max(1, Math.ceil(L / (r * (paso || 0.7))));
        for (k = 0; k < m; k++) pts.push(cr(p0, p1, p2, p3, k / m));
      }
      pts.push(P[n - 1]);
      pts.forEach(function (q, idx) {
        var col = typeof c === 'function' ? c(pts.length > 1 ? idx / (pts.length - 1) : 0) : c;
        S.esfera(q[0], q[1], q[2], r, col);
      });
      return S;
    };

    S.iniciar = function () {
      function go() {
        var cv = document.createElement('canvas');
        var st = document.body.style;
        st.margin = '0';
        st.background = S.fondo;
        st.overflow = 'hidden';
        cv.style.cssText = 'display:block;width:100vw;height:100vh;touch-action:none';
        document.body.appendChild(cv);
        var g = cv.getContext('2d');
        var w = 0, h = 0, dpr = Math.min(W.devicePixelRatio || 1, 2);

        var mn = [1e9, 1e9, 1e9], mx = [-1e9, -1e9, -1e9], any = false;
        function ext(p, r) {
          any = true;
          for (var q = 0; q < 3; q++) {
            if (p[q] - r < mn[q]) mn[q] = p[q] - r;
            if (p[q] + r > mx[q]) mx[q] = p[q] + r;
          }
        }
        esf.forEach(function (e) { ext(e.p, e.r); });
        pol.forEach(function (p) { p.v.forEach(function (q) { ext(q, 0); }); });
        lin.forEach(function (l) { ext(l.a, 0); ext(l.b, 0); });
        txt.forEach(function (t) { ext(t.p, 0); });
        if (!any) { mn = [-1, -1, -1]; mx = [1, 1, 1]; }
        var C = [(mn[0] + mx[0]) / 2, (mn[1] + mx[1]) / 2, (mn[2] + mx[2]) / 2];
        var R = Math.sqrt(Math.pow(mx[0] - mn[0], 2) + Math.pow(mx[1] - mn[1], 2) + Math.pow(mx[2] - mn[2], 2)) / 2;
        if (!(R > 0)) R = 1;
        var D = R * 3;

        var LX = -0.45, LY = 0.55, LZ = -0.7, ll = Math.sqrt(LX * LX + LY * LY + LZ * LZ);
        LX /= ll; LY /= ll; LZ /= ll;

        var ptrs = {}, tocando = false, last = 0;

        cv.addEventListener('pointerdown', function (e) {
          if (cv.setPointerCapture) cv.setPointerCapture(e.pointerId);
          ptrs[e.pointerId] = [e.clientX, e.clientY];
          tocando = true;
        });
        cv.addEventListener('pointermove', function (e) {
          var p = ptrs[e.pointerId];
          if (!p) return;
          var ids = Object.keys(ptrs);
          if (ids.length === 2) {
            var o = ptrs[ids[0] == e.pointerId ? ids[1] : ids[0]];
            var dn = Math.sqrt(Math.pow(e.clientX - o[0], 2) + Math.pow(e.clientY - o[1], 2));
            var dv = Math.sqrt(Math.pow(p[0] - o[0], 2) + Math.pow(p[1] - o[1], 2));
            if (dv > 0) S.zoom = Math.max(0.3, Math.min(6, S.zoom * dn / dv));
          } else {
            S.yaw -= (e.clientX - p[0]) * 0.01;
            S.pitch = Math.max(-1.5, Math.min(1.5, S.pitch + (e.clientY - p[1]) * 0.01));
          }
          ptrs[e.pointerId] = [e.clientX, e.clientY];
        });
        function up(e) {
          delete ptrs[e.pointerId];
          if (!Object.keys(ptrs).length) tocando = false;
        }
        cv.addEventListener('pointerup', up);
        cv.addEventListener('pointercancel', up);
        cv.addEventListener('wheel', function (e) {
          e.preventDefault();
          S.zoom = Math.max(0.3, Math.min(6, S.zoom * (e.deltaY < 0 ? 1.1 : 0.9)));
        }, { passive: false });

        function frame(t) {
          var cw = cv.clientWidth, ch = cv.clientHeight;
          if (cw !== w || ch !== h) {
            w = cw; h = ch;
            cv.width = Math.max(1, Math.round(w * dpr));
            cv.height = Math.max(1, Math.round(h * dpr));
          }
          var dt = last ? Math.min(0.05, (t - last) / 1000) : 0.016;
          last = t;
          if (S.rotar && !tocando) S.yaw += S.vel * dt;

          g.setTransform(dpr, 0, 0, dpr, 0, 0);
          g.fillStyle = S.fondo;
          g.fillRect(0, 0, w, h);

          var F = 1.26 * Math.min(w, h) * S.zoom;
          var cy = Math.cos(S.yaw), sy = Math.sin(S.yaw), cp = Math.cos(S.pitch), sp = Math.sin(S.pitch);
          var ox = w / 2, oy = h / 2;

          function tr(p) {
            var x = p[0] - C[0], y = p[1] - C[1], z = p[2] - C[2];
            var x1 = cy * x + sy * z, z1 = -sy * x + cy * z;
            return [x1, cp * y + sp * z1, -sp * y + cp * z1 + D];
          }

          var Q = [];
          esf.forEach(function (e) {
            var q = tr(e.p), s = F / q[2];
            Q.push({ k: 0, z: q[2], x: ox + q[0] * s, y: oy - q[1] * s, r: e.r * s, s: e.s });
          });
          pol.forEach(function (p) {
            var v = p.v.map(tr), a = v[0], b = v[1], c2 = v[2];
            var ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            var vx = c2[0] - a[0], vy = c2[1] - a[1], vz = c2[2] - a[2];
            var nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            var ln = Math.sqrt(nx * nx + ny * ny + nz * nz) || 1;
            nx /= ln; ny /= ln; nz /= ln;
            if (nz > 0) { nx = -nx; ny = -ny; nz = -nz; }
            var f = 0.3 + 0.7 * Math.max(0, nx * LX + ny * LY + nz * LZ);
            var zc = 0;
            v.forEach(function (q) { zc += q[2]; });
            Q.push({
              k: 1, z: zc / v.length,
              pts: v.map(function (q) { var s = F / q[2]; return [ox + q[0] * s, oy - q[1] * s]; }),
              col: css([p.c[0] * f, p.c[1] * f, p.c[2] * f])
            });
          });
          lin.forEach(function (l) {
            var a = tr(l.a), b = tr(l.b), sa = F / a[2], sb = F / b[2], z = (a[2] + b[2]) / 2;
            Q.push({
              k: 2, z: z, x1: ox + a[0] * sa, y1: oy - a[1] * sa, x2: ox + b[0] * sb, y2: oy - b[1] * sb,
              col: l.c, w: l.w * D / z
            });
          });
          txt.forEach(function (t) {
            var q = tr(t.p), s = F / q[2];
            Q.push({ k: 3, z: q[2], x: ox + q[0] * s, y: oy - q[1] * s, t: t.t, col: t.c, sz: t.sz * D / q[2] });
          });
          Q.sort(function (a, b) { return b.z - a.z; });

          var z0 = D - R, span = (2 * R) || 1;
          g.textAlign = 'center';
          Q.forEach(function (o) {
            var near = Math.max(0, Math.min(1, 1 - (o.z - z0) / span));
            if (o.k === 1) {
              g.globalAlpha = 1;
              g.fillStyle = o.col;
              g.strokeStyle = o.col;
              g.lineWidth = 0.8;
              g.beginPath();
              o.pts.forEach(function (p, i) { if (i) g.lineTo(p[0], p[1]); else g.moveTo(p[0], p[1]); });
              g.closePath();
              g.fill();
              g.stroke();
              return;
            }
            g.globalAlpha = 0.6 + 0.4 * near;
            if (o.k === 0) {
              g.drawImage(o.s, o.x - o.r, o.y - o.r, 2 * o.r, 2 * o.r);
            } else if (o.k === 2) {
              g.strokeStyle = o.col;
              g.lineWidth = o.w;
              g.lineCap = 'round';
              g.beginPath();
              g.moveTo(o.x1, o.y1);
              g.lineTo(o.x2, o.y2);
              g.stroke();
            } else {
              g.fillStyle = o.col;
              g.font = Math.max(8, o.sz) + 'px sans-serif';
              g.fillText(o.t, o.x, o.y);
            }
          });
          g.globalAlpha = 1;
          W.requestAnimationFrame(frame);
        }
        W.requestAnimationFrame(frame);
      }
      if (document.body) go();
      else document.addEventListener('DOMContentLoaded', go);
      return S;
    };

    return S;
  }

  W.Argon3D = {
    escena: escena,
    crear: escena,
    hsl: function (h, s, l) { return 'hsl(' + Math.round(h) + ',' + Math.round(s) + '%,' + Math.round(l) + '%)'; }
  };
})();
"""
}
