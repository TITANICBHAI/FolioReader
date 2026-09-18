
const menu=document.querySelector('.menu');
const links=document.querySelector('.nav-links');
if(menu&&links){menu.addEventListener('click',()=>{links.style.display=links.style.display==='flex'?'none':'flex'; if(links.style.display==='flex'){links.style.position='absolute';links.style.left='12px';links.style.right='12px';links.style.top='68px';links.style.padding='16px';links.style.background='#fbf8f2';links.style.border='1px solid #ddd4c6';links.style.borderRadius='14px';links.style.flexDirection='column';links.style.boxShadow='0 15px 40px rgba(16,24,39,.1)'}})}
const year=document.querySelectorAll('[data-year]'); year.forEach(x=>x.textContent=new Date().getFullYear());
