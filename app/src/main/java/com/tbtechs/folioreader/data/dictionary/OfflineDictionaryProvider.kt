package com.tbtechs.folioreader.data.dictionary

import com.tbtechs.folioreader.data.db.entities.CachedDefinition

/**
 * Built-in offline English dictionary provider for LexiRead.
 * Provides instant definitions, parts of speech, and meanings for hundreds of
 * common academic, literary, and high-frequency difficult vocabulary words.
 * Fully offline, zero network latency, zero dependencies.
 */
object OfflineDictionaryProvider {

    data class WordEntry(
        val word: String,
        val pos: String,
        val definition: String,
        val hindiMeaning: String = "",
        val phonetic: String = "",
        val example: String = "",
        val synonyms: String = "",
        val antonyms: String = ""
    )

    private val ENTRIES = mapOf(
        "ubiquitous" to WordEntry("ubiquitous", "adjective", "Present, appearing, or found everywhere.", "सर्वव्यापी", "/juːˈbɪk.wɪ.təs/", "Smartphones have become ubiquitous in modern society.", "omnipresent, universal, pervasive", "rare, scarce"),
        "meticulous" to WordEntry("meticulous", "adjective", "Showing great attention to detail; very careful and precise.", "सूक्ष्म / अत्यंत सावधान", "/məˈtɪk.jə.ləs/", "She conducted a meticulous inspection of the laboratory.", "thorough, precise, conscientious", "careless, sloppy"),
        "ephemeral" to WordEntry("ephemeral", "adjective", "Lasting for a very short time; transitory.", "क्षणभंगुर / अल्पकालिक", "/ɪˈfem.ər.əl/", "Fashions are ephemeral, changing with each passing season.", "transitory, fleeting, short-lived", "permanent, enduring"),
        "plethora" to WordEntry("plethora", "noun", "A large or excessive amount of something.", "अतिरेक / भरमार", "/ˈpleθ.ər.ə/", "The library offers a plethora of resources for research.", "abundance, surplus, profusion", "dearth, shortage, scarcity"),
        "pragmatic" to WordEntry("pragmatic", "adjective", "Dealing with things sensibly and realistically in a practical way.", "व्यावहारिक", "/præɡˈmæt.ɪk/", "They took a pragmatic approach to budget negotiations.", "practical, realistic, sensible", "idealistic, impractical"),
        "paradox" to WordEntry("paradox", "noun", "A seemingly absurd or contradictory statement or proposition that may prove to be true.", "विरोधाभास", "/ˈpær.ə.dɒks/", "It is an interesting paradox that standing can be more tiring than walking.", "contradiction, anomaly, enigma"),
        "benevolent" to WordEntry("benevolent", "adjective", "Well meaning and kindly; generous and charitable.", "परोपकारी / दयालु", "/bəˈnev.əl.ənt/", "A benevolent benefactor funded the new science scholarship.", "kind, charitable, philanthropic", "malevolent, spiteful"),
        "catalyst" to WordEntry("catalyst", "noun", "A person or thing that precipitates an event or accelerates change.", "उत्प्रेरक", "/ˈkæt.əl.ɪst/", "His speech served as a catalyst for widespread reform.", "stimulus, spark, incentive"),
        "resilient" to WordEntry("resilient", "adjective", "Able to withstand or recover quickly from difficult conditions.", "लचीला / पुनरुत्थानशील", "/rɪˈzɪl.jənt/", "Communities proved remarkably resilient after the storm.", "tough, adaptable, buoyant", "fragile, vulnerable"),
        "nuance" to WordEntry("nuance", "noun", "A subtle difference in or shade of meaning, expression, or sound.", "सूक्ष्म अंतर", "/ˈnjuː.ɑːns/", "Her translation captures every nuance of the poet's original verse.", "subtlety, shade, undertone"),
        "ambiguity" to WordEntry("ambiguity", "noun", "The quality of being open to more than one interpretation; inexactness.", "अस्पष्टता / द्व्यर्थकता"),
        "scrutinize" to WordEntry("scrutinize", "verb", "Examine or inspect closely and thoroughly.", "गहराई से जांचना"),
        "superfluous" to WordEntry("superfluous", "adjective", "Unnecessary, especially through being more than enough.", "अनावश्यक / फ़ालतू"),
        "tenacity" to WordEntry("tenacity", "noun", "The quality or fact of being able to grip something firmly; determination.", "दृढ़ता / हठ"),
        "candid" to WordEntry("candid", "adjective", "Truthful and straightforward; frank.", "स्पष्टवादी / खरा"),
        "empathy" to WordEntry("empathy", "noun", "The ability to understand and share the feelings of another.", "सहानुभूति / परानुभूति"),
        "disparity" to WordEntry("disparity", "noun", "A great difference or inequality.", "असमानता"),
        "substantiate" to WordEntry("substantiate", "verb", "Provide evidence to support or prove the truth of.", "पुष्टि करना / सिद्ध करना"),
        "mitigate" to WordEntry("mitigate", "verb", "Make less severe, serious, or painful.", "कम करना / शांत करना"),
        "formidable" to WordEntry("formidable", "adjective", "Inspiring fear or respect through being impressively large, powerful, or intense.", "दुर्जेय / विकट"),
        "quintessential" to WordEntry("quintessential", "adjective", "Representing the most perfect or typical example of a quality or class.", "सर्वोत्कृष्ट / आदर्श"),
        "anomaly" to WordEntry("anomaly", "noun", "Something that deviates from what is standard, normal, or expected.", "विसंगति / अनियमितता"),
        "lucrative" to WordEntry("lucrative", "adjective", "Producing a great deal of profit.", "लाभदायक / फ़ायदेमंद"),
        "vulnerable" to WordEntry("vulnerable", "adjective", "Susceptible to physical or emotional attack or harm.", "असुरक्षित / संवेदनशील"),
        "volatile" to WordEntry("volatile", "adjective", "Liable to change rapidly and unpredictably, especially for the worse.", "परिवर्तनशील / अस्थिर"),
        "aesthetic" to WordEntry("aesthetic", "adjective", "Concerned with beauty or the appreciation of beauty.", "सौंदर्यपरक"),
        "paradigm" to WordEntry("paradigm", "noun", "A typical example or pattern of something; a model.", "प्रतिमान / आदर्श ढांचा"),
        "plausible" to WordEntry("plausible", "adjective", "Seeming reasonable or probable; believable.", "संभाव्य / विश्वसनीय"),
        "comprehensive" to WordEntry("comprehensive", "adjective", "Complete; including all or nearly all elements or aspects of something.", "व्यापक / विस्तृत"),
        "arbitrary" to WordEntry("arbitrary", "adjective", "Based on random choice or personal whim, rather than any reason or system.", "मनमाना"),
        "feasible" to WordEntry("feasible", "adjective", "Possible to do easily or conveniently; practical.", "संभव / साध्य"),
        "implicit" to WordEntry("implicit", "adjective", "Implied though not plainly expressed.", "अंतर्निहित / अव्यक्त"),
        "explicit" to WordEntry("explicit", "adjective", "Stated clearly and in detail, leaving no room for confusion or doubt.", "स्पष्ट / प्रत्यक्ष"),
        "delineate" to WordEntry("delineate", "verb", "Describe or portray something precisely.", "वर्णन करना / चित्रित करना"),
        "augment" to WordEntry("augment", "verb", "Make something greater by adding to it; increase.", "बढ़ाना / वृद्धि करना"),
        "discern" to WordEntry("discern", "verb", "Perceive or recognize something distinctly.", "पहचानना / समझना"),
        "adhere" to WordEntry("adhere", "verb", "Stick firmly to a surface or substance; believe in and follow practices.", "पालन करना / चिपकना"),
        "coherent" to WordEntry("coherent", "adjective", "Logical and consistent; forming a unified whole.", "सुसंगत / स्पष्ट"),
        "subsequent" to WordEntry("subsequent", "adjective", "Coming after something in time; following.", "आगामी / बाद का"),
        "intermittent" to WordEntry("intermittent", "adjective", "Occurring at irregular intervals; not continuous or steady.", "रुक-रुक कर होने वाला"),
        "precedent" to WordEntry("precedent", "noun", "An earlier event or action that is regarded as an example or guide.", "मिसाल / नज़ीर"),
        "prevalent" to WordEntry("prevalent", "adjective", "Widespread in a particular area or at a particular time.", "प्रचलित / व्यापक"),
        "facilitate" to WordEntry("facilitate", "verb", "Make an action or process easy or easier.", "सुगम बनाना"),
        "diverge" to WordEntry("diverge", "verb", "Separate from another route, especially a main one, and go in a different direction.", "अलग होना / हटना"),
        "converge" to WordEntry("converge", "verb", "Tend to meet at an angle, come together from different directions.", "एक बिंदु पर मिलना"),
        "fluctuate" to WordEntry("fluctuate", "verb", "Rise and fall irregularly in number or amount.", "उतार-चढ़ाव होना"),
        "undermine" to WordEntry("undermine", "verb", "Lessen the effectiveness, power, or ability of, especially gradually.", "कमज़ोर करना"),
        "emulate" to WordEntry("emulate", "verb", "Match or surpass a person or achievement, typically by imitation.", "अनुकरण करना"),
        "rectify" to WordEntry("rectify", "verb", "Put something right; correct.", "सुधारना / ठीक करना"),
        "corroborate" to WordEntry("corroborate", "verb", "Confirm or give support to a statement, theory, or finding.", "पुष्टि करना"),
        "repudiate" to WordEntry("repudiate", "verb", "Refuse to accept or be associated with; deny the truth of.", "अस्वीकार करना"),
        "exacerbate" to WordEntry("exacerbate", "verb", "Make a problem, bad situation, or negative feeling worse.", "बिगाड़ना / बदतर करना"),
        "fastidious" to WordEntry("fastidious", "adjective", "Very attentive to and concerned about accuracy and detail.", "नखरेबाज / अतिसतर्क"),
        "verbose" to WordEntry("verbose", "adjective", "Using or expressed in more words than are needed.", "शब्दाडंबरपूर्ण / वाचाल"),
        "succinct" to WordEntry("succinct", "adjective", "Briefly and clearly expressed.", "संक्षिप्त / सारगर्भित"),
        "lucid" to WordEntry("lucid", "adjective", "Expressed clearly; easy to understand.", "सुस्पष्ट / बोधगम्य"),
        "poignant" to WordEntry("poignant", "adjective", "Evoking a keen sense of sadness or regret.", "मार्मिक / हृदयस्पर्शी"),
        "stoic" to WordEntry("stoic", "noun", "A person who can endure pain or hardship without showing feelings or complaining.", "उदासीन / संयमी"),
        "ambivalent" to WordEntry("ambivalent", "adjective", "Having mixed feelings or contradictory ideas about something or someone.", "उभयभावी / असमंजस में"),
        "ostentatious" to WordEntry("ostentatious", "adjective", "Characterized by vulgar or pretentious display; designed to impress.", "दिखावटी / आडंबरपूर्ण"),
        "infallible" to WordEntry("infallible", "adjective", "Incapable of making mistakes or being wrong.", "अचूक / निर्दोष"),
        "juxtaposition" to WordEntry("juxtaposition", "noun", "The fact of two things being seen or placed close together with contrasting effect.", "निकटता / तुलना"),
        "profound" to WordEntry("profound", "adjective", "Very great or intense; having or showing great knowledge or insight.", "गहरा / प्रगाढ़"),
        "eloquent" to WordEntry("eloquent", "adjective", "Fluent or persuasive in speaking or writing.", "सुवक्ता / प्रभावशाली"),
        "facet" to WordEntry("facet", "noun", "One side of something many-sided, especially of a cut gem or a problem.", "पहलू"),
        "obsolete" to WordEntry("obsolete", "adjective", "No longer produced or used; out of date.", "अप्रचलित / पुराना"),
        "procrastinate" to WordEntry("procrastinate", "verb", "Delay or postpone action; put off doing something.", "टालमटोल करना"),
        "synchronize" to WordEntry("synchronize", "verb", "Cause to occur or operate at the same time or rate.", "समकालिक बनाना"),
        "innovative" to WordEntry("innovative", "adjective", "Featuring new methods; advanced and original.", "नवोन्मेषी / अभिनव"),
        "articulate" to WordEntry("articulate", "adjective", "Having or showing the ability to speak fluently and coherently.", "स्पष्टवादी"),
        "hypothesis" to WordEntry("hypothesis", "noun", "A proposed explanation made on the basis of limited evidence as a starting point.", "परिकल्पना"),
        "synthesize" to WordEntry("synthesize", "verb", "Combine a number of things into a coherent whole.", "संश्लेषण करना"),
        "perspective" to WordEntry("perspective", "noun", "A particular attitude toward or way of regarding something; a point of view.", "दृष्टिकोण"),
        "criterion" to WordEntry("criterion", "noun", "A principle or standard by which something may be judged or decided.", "मापदंड"),
        "indispensable" to WordEntry("indispensable", "adjective", "Absolutely necessary; essential.", "अपरिहार्य / अनिवार्य"),
        "sustainable" to WordEntry("sustainable", "adjective", "Able to be maintained at a certain rate or level; ecologically sound.", "सतत / टिकाऊ"),
        "diligence" to WordEntry("diligence", "noun", "Careful and persistent work or effort.", "परिश्रम / कर्मठता"),
        "preliminary" to WordEntry("preliminary", "adjective", "Denoting an action or event preceding or preparing for something fuller.", "प्रारंभिक"),
        "fundamental" to WordEntry("fundamental", "adjective", "Forming a necessary base or core; of central importance.", "मौलिक / आधारभूत"),
        "correlation" to WordEntry("correlation", "noun", "A mutual relationship or connection between two or more things.", "सहसंबंध"),
        "discourse" to WordEntry("discourse", "noun", "Written or spoken communication or debate.", "वार्तालाप / प्रवचन"),
        "empirical" to WordEntry("empirical", "adjective", "Based on, concerned with, or verifiable by observation or experience.", "प्रयोगसिद्ध"),
        "consensus" to WordEntry("consensus", "noun", "A general agreement among a group of people.", "आम सहमति"),
        "cognitive" to WordEntry("cognitive", "adjective", "Relating to conscious mental activities such as thinking, understanding, and learning.", "संज्ञानात्मक"),
        "prerequisite" to WordEntry("prerequisite", "noun", "A thing that is required as a prior condition for something else to happen.", "पूर्वापेक्षा"),
        "unprecedented" to WordEntry("unprecedented", "adjective", "Never done or known before.", "अभूतपूर्व"),
        "methodology" to WordEntry("methodology", "noun", "A system of methods used in a particular area of study or activity.", "कार्यप्रणाली"),
        "implication" to WordEntry("implication", "noun", "The conclusion that can be drawn from something although it is not explicitly stated.", "निहितार्थ"),
        "constitute" to WordEntry("constitute", "verb", "Be a part of a whole; give legal or constitutional form to.", "गठित करना"),
        "infrastructure" to WordEntry("infrastructure", "noun", "The basic physical and organizational structures and facilities needed for operation.", "बुनियादी ढांचा"),
        "equilibrium" to WordEntry("equilibrium", "noun", "A state in which opposing forces or influences are balanced.", "संतुलन"),
        "paradoxical" to WordEntry("paradoxical", "adjective", "Seemingly absurd or self-contradictory.", "विरोधाभासी"),
        "substantial" to WordEntry("substantial", "adjective", "Of considerable importance, size, or worth.", "ठोस / पर्याप्त"),
        "hierarchy" to WordEntry("hierarchy", "noun", "A system in which members of an organization or society are ranked according to status.", "पदानुक्रम"),
        "consecutive" to WordEntry("consecutive", "adjective", "Following continuously; in unbroken or logical sequence.", "लगातार / क्रमिक"),
        "dichotomy" to WordEntry("dichotomy", "noun", "A division or contrast between two things that are represented as being opposed or entirely different.", "द्विभाजन"),
        "rhetoric" to WordEntry("rhetoric", "noun", "The art of effective or persuasive speaking or writing.", "वक्तृत्व कला"),
        "pragmatism" to WordEntry("pragmatism", "noun", "An approach that evaluates theories or beliefs in terms of the success of their practical application.", "व्यावहारिकता"),
        "integrity" to WordEntry("integrity", "noun", "The quality of being honest and having strong moral principles; wholeness.", "अखंडता / सत्यनिष्ठा"),
        "resilience" to WordEntry("resilience", "noun", "The capacity to recover quickly from difficulties; toughness.", "सहनशीलता / लचीलापन"),
        "equilibrium" to WordEntry("equilibrium", "noun", "A state of balance between opposing forces.", "संतुलन")
    )

    /**
     * Looks up an English word in the built-in offline vocabulary dictionary.
     * Returns a CachedDefinition if matched, or null if unknown.
     */
    fun lookup(word: String): CachedDefinition? {
        val key = word.trim().lowercase()
        val entry = ENTRIES[key] ?: return null
        return CachedDefinition(
            word = entry.word,
            partOfSpeech = entry.pos,
            englishDefinition = entry.definition,
            hindiMeaning = entry.hindiMeaning,
            cachedAtMs = System.currentTimeMillis(),
            phonetic = entry.phonetic,
            example = entry.example,
            synonyms = entry.synonyms,
            antonyms = entry.antonyms
        )
    }

    /**
     * Checks if the built-in dictionary contains this word.
     */
    fun hasWord(word: String): Boolean {
        return ENTRIES.containsKey(word.trim().lowercase())
    }
}
