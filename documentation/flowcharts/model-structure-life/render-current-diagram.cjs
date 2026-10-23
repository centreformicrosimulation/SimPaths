// Render the user-owned ClickCharts geometry as an accessible web SVG.
const fs = require('node:fs');
const geometry = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const out = process.argv[3];
const esc=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;');
const circles=new Set([0,1,2,3,4,20,24,36,48,54,68]);
const round=new Set([15,21,66]);
const purple=new Set([66,69,79,81]);
const labels={5:['Ageing'],6:['Partnership'],7:['Fertility'],8:['t+1'],9:['Household','composition','module'],10:['Demographic','module'],11:['Educational','achievement'],12:['Education','module'],13:['Hours supplied'],14:['Taxes & Benefits'],15:['Potential income'],19:['Labour supply','module'],22:['Wages'],25:['Retirement'],26:['Public pensions'],27:['Student status'],28:['Health','module'],29:['Physical health'],30:['Disability'],31:['Retirement'],33:['Leaving parental','home'],37:['Consumption'],38:['Income and','consumption','module'],39:['Mental health'],40:['Death'],41:['Internal mobility'],42:['External migration'],44:['Start'],46:['Past income','trajectories'],49:['Wealth','module'],50:['Housing'],51:['Private pensions'],52:['Other assets'],55:['Macro','module'],56:['Long term','growth'],59:['Social care','module'],60:['Care needs'],61:['Informal care'],62:['Social care'],65:['Life satisfaction'],66:['Pension rules'],69:['Inter-vivos','transfers'],70:['Disposable income'],71:['Childcare costs'],72:['Home production'],75:['Bequests'],76:['Well-being','module'],77:['Private monetary','transfers module'],79:['Other well-being','outcomes'],80:['Debt'],81:['Short term','fluctuations']};
const headings=new Set([9,10,12,19,28,31,38,49,55,59,76,77]);
const textOnly=new Set([...headings,8,43]);
const p=['<svg xmlns="http://www.w3.org/2000/svg" width="1400" height="1078" viewBox="-28 -115 365 281" role="img" aria-labelledby="title desc">',
'<title id="title">SimPaths UK model structure</title>',
'<desc id="desc">Conceptual annual module cycle. The wealth module includes housing, private pensions, other assets and debt. The macro module distinguishes long term growth from short term fluctuations. Purple dashed outlines indicate components under development: short term fluctuations, other well-being outcomes, inter-vivos transfers and pension-rule imputation. Long term growth uses external economic assumptions. Circles group processes; arrows do not specify the exact execution schedule.</desc>',
'<defs><marker id="arrow" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto" markerUnits="userSpaceOnUse"><path d="M0 0L5 2.5L0 5Z" fill="#263238"/></marker></defs>',
'<rect x="-28" y="-115" width="365" height="281" fill="white"/>',
'<g stroke="#263238" stroke-width=".28" fill="none" marker-end="url(#arrow)">'];
for(const d of ['M204.43 -89.36V-74.08','M149.06 -93.77V-84H204.43','M229.36 -49.15H279.5V-15.88','M254.2 9.42H228.25','M178.87 9.42H153.18','M128.49 34.11V46.68','M154.31 72.5H180.04','M229.42 72.5H250.2','M282.22 103.5V110.23','M257.73 134.92H224.09','M104.11 134.92H63.68','M38.99 110.23V97','M38.99 45V34.5','M38.99 -15.18V-49.15H179.5']) p.push(`<path d="${d}"/>`);
p.push('</g>');
for(const g of [...geometry].sort((a,b)=>(b.index===21?1:0)-(a.index===21?1:0))){
 const i=g.index,x=g['0x1510'],y=g['0x1511'],w=([42,72].includes(i)?g['0x1512']+1.5:g['0x1512']),h=g['0x1513'];
 if(x===undefined||textOnly.has(i))continue;
 const colour=purple.has(i)?'#85538f':'#263238';
 const style=`fill="white" stroke="${colour}" stroke-width=".4"${purple.has(i)?' stroke-dasharray="1.5 .8"':''}`;
 if(circles.has(i))p.push(`<ellipse cx="${x}" cy="${y}" rx="${w}" ry="${h}" ${style}/>`);
 else if(i===44)p.push(`<path d="M${x} ${y-h}L${x+w} ${y}L${x} ${y+h}L${x-w} ${y}Z" ${style}/>`);
 else p.push(`<rect x="${x-w}" y="${y-h}" width="${2*w}" height="${2*h}" rx="${round.has(i)?Math.min(w,h):0}" ${style}/>`);
}
// Internal labour-supply links lie over the enclosing module boundary.
p.push('<g stroke="#263238" stroke-width=".28" fill="none" marker-end="url(#arrow)"><path d="M225.78 106H204.43V84.66"/><path d="M193.63 129.58V140.8"/><path d="M175.63 145.99H158"/><path d="M137.14 142.04V129.58"/></g>');
for(const g of geometry){
 const i=g.index,lines=labels[i]; if(!lines)continue;
 const x=g['0x1510'],y=g['0x1511'];
 const size=headings.has(i)?4.5:4.4, line=4.7;
 p.push(`<text x="${x}" y="${y-(lines.length-1)*line/2}" text-anchor="middle" dominant-baseline="middle" font-family="Arial, sans-serif" font-size="${size}" font-weight="${headings.has(i)?700:400}" fill="#18252b">${lines.map((s,j)=>`<tspan x="${x}" dy="${j?line:0}">${esc(s)}</tspan>`).join('')}</text>`);
}
p.push('<g font-family="Arial,sans-serif" fill="#18252b"><text x="-18" y="-102" font-size="5.4" font-weight="700">SimPaths UK</text><text x="-18" y="-95" font-size="4">Model structure · October 2026</text>');
for(const [j,color,label,dash] of [[0,'#85538f','Under development','1.5 .8']]) {
 const y=-87+j*6;
 p.push(`<path d="M-18 ${y}H-10" stroke="${color}" stroke-width=".7" ${dash?`stroke-dasharray="${dash}"`:''}/><text x="-7" y="${y+1.1}" font-size="4">${label}</text>`);
}
p.push('</g></svg>');
fs.writeFileSync(out,p.join('\n'));
