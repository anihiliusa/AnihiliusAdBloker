package com.anihiliusa.xtube

object CleanerScripts {
    fun cleanPage(darkMode: Boolean): String = """
        (function() {
            const selectors = ['.ad','.ads','.advert','.advertisement','.banner','.popup','.sponsor','.sponsored','.promoted','[class*=advert]','[class*=sponsor]','iframe[src*=doubleclick]','ins.adsbygoogle','ytd-ad-slot-renderer','ytd-display-ad-renderer','.ytp-ad-module','.video-ads'];
            function hide(n){ try{ n.remove(); }catch(e){ try{n.style.display='none'}catch(x){} } }
            function run(){
                selectors.forEach(function(s){ try{ document.querySelectorAll(s).forEach(hide); }catch(e){} });
                if(!document.getElementById('xtube-style')){
                    var st=document.createElement('style'); st.id='xtube-style';
                    st.textContent='${if (darkMode) "html,body{background:#0b0b0f!important;}" else ""} .ad,.ads,.advertisement,.sponsored,.promoted,ytd-ad-slot-renderer,ytd-display-ad-renderer,.ytp-ad-module,.video-ads{display:none!important;}';
                    document.documentElement.appendChild(st);
                }
            }
            run();
            if(!window.__xtube){ window.__xtube=true; setInterval(run,1200); try{new MutationObserver(run).observe(document.documentElement||document.body,{childList:true,subtree:true});}catch(e){} }
        })();
    """.trimIndent()
}
