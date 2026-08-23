package com.example.data.dictionary

import com.example.data.model.DictionaryEntry
import com.example.data.model.DictionaryMeaning

object DefaultDictionaryData {
    val entries: Map<String, DictionaryEntry> by lazy {
        val list = listOf(
            DictionaryEntry(
                word = "ephemeral",
                phonetic = "/ɪˈfem.ər.əl/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Lasting for only a very short time; transitory; fleeting."),
                        examples = listOf("The ephemeral beauty of cherry blossoms in springtime.", "Fame in modern media is often ephemeral.")
                    )
                ),
                origin = "From Greek ephēmeros, meaning 'lasting only a day'."
            ),
            DictionaryEntry(
                word = "serendipity",
                phonetic = "/ˌser.ənˈdɪp.ə.ti/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The occurrence and development of events by chance in a happy or beneficial way."),
                        examples = listOf("Finding that rare first edition book was pure serendipity.")
                    )
                ),
                origin = "Coined by Horace Walpole in 1754 from the Persian fairy tale 'The Three Princes of Serendip'."
            ),
            DictionaryEntry(
                word = "solitude",
                phonetic = "/ˈsɒl.ɪ.tjuːd/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The state or situation of being alone, especially when pleasant and peaceful."),
                        examples = listOf("She savored the peace and solitude of the mountain cabin.")
                    )
                )
            ),
            DictionaryEntry(
                word = "eloquence",
                phonetic = "/ˈel.ə.kwəns/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("Fluent or persuasive speaking or writing; the art of using language with clarity and power."),
                        examples = listOf("The senator spoke with great eloquence and conviction.")
                    )
                )
            ),
            DictionaryEntry(
                word = "resilience",
                phonetic = "/rɪˈzɪl.jəns/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The capacity to recover quickly from difficulties; toughness and elasticity."),
                        examples = listOf("The resilience of the human spirit in times of crisis.")
                    )
                )
            ),
            DictionaryEntry(
                word = "luminous",
                phonetic = "/ˈluː.mɪ.nəs/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Full of or shedding light; bright or shining, especially in the dark; clear and enlightened."),
                        examples = listOf("The luminous moon reflected over the tranquil sea.", "Her luminous prose captivated generations of readers.")
                    )
                )
            ),
            DictionaryEntry(
                word = "ineffable",
                phonetic = "/ɪnˈef.ə.bəl/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Too great or extreme to be expressed or described in words; inexpressible."),
                        examples = listOf("An ineffable sense of joy swept through the crowd.")
                    )
                )
            ),
            DictionaryEntry(
                word = "melancholy",
                phonetic = "/ˈmel.əŋ.kɒl.i/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun / adjective",
                        definitions = listOf("A feeling of pensive sadness, typically with no obvious cause; deeply thoughtful and somber."),
                        examples = listOf("A feeling of melancholy descended on him as autumn arrived.")
                    )
                )
            ),
            DictionaryEntry(
                word = "stratagem",
                phonetic = "/ˈstræt.ə.dʒəm/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A plan or scheme, especially one used to outwit an opponent or achieve an end in warfare or strategy."),
                        examples = listOf("Sun Tzu's brilliant stratagem secured victory before the battle even began.")
                    )
                )
            ),
            DictionaryEntry(
                word = "sublime",
                phonetic = "/səˈblaɪm/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Of such excellence, grandeur, or beauty as to inspire great admiration or awe."),
                        examples = listOf("The orchestra gave a sublime performance of Beethoven's Ninth.")
                    )
                )
            ),
            DictionaryEntry(
                word = "fortitude",
                phonetic = "/ˈfɔː.tɪ.tjuːd/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("Courage in pain or adversity."),
                        examples = listOf("She endured her long illness with admirable fortitude.")
                    )
                )
            ),
            DictionaryEntry(
                word = "epiphany",
                phonetic = "/ɪˈpɪf.ən.i/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A moment of sudden revelation or profound insight."),
                        examples = listOf("In the quiet library, he experienced a sudden epiphany that solved the problem.")
                    )
                )
            ),
            DictionaryEntry(
                word = "pragmatic",
                phonetic = "/præɡˈmæt.ɪk/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Dealing with things sensibly and realistically in a way based on practical considerations rather than theoretical ones."),
                        examples = listOf("We need a pragmatic approach to solve this dilemma.")
                    )
                )
            ),
            DictionaryEntry(
                word = "quixotic",
                phonetic = "/kwɪkˈsɒt.ɪk/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Exceedingly idealistic; unrealistic and impractical."),
                        examples = listOf("A vast, quixotic campaign to change the world overnight.")
                    )
                ),
                origin = "Derived from Don Quixote, hero of the romance by Cervantes."
            ),
            DictionaryEntry(
                word = "curious",
                phonetic = "/ˈkjʊə.ri.əs/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Eager to know or learn something; strange, unusual or noteworthy."),
                        examples = listOf("'Curiouser and curiouser!' cried Alice.")
                    )
                )
            ),
            DictionaryEntry(
                word = "labyrinth",
                phonetic = "/ˈlæb.ə.rɪnθ/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A complicated irregular network of passages or paths in which it is difficult to find one's way; a maze."),
                        examples = listOf("A labyrinth of narrow streets in the old city.")
                    )
                )
            ),
            DictionaryEntry(
                word = "sagacity",
                phonetic = "/səˈɡæs.ə.ti/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The quality of being sagacious; having or showing keen mental discernment and good judgement; shrewdness."),
                        examples = listOf("A man of great political sagacity.")
                    )
                )
            ),
            DictionaryEntry(
                word = "paradox",
                phonetic = "/ˈpær.ə.dɒks/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A seemingly absurd or self-contradictory statement or proposition that when investigated may prove to be well founded or true."),
                        examples = listOf("The liar paradox: 'This statement is false.'")
                    )
                )
            ),
            DictionaryEntry(
                word = "chronometer",
                phonetic = "/krəˈnɒm.ɪ.tər/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("An instrument for measuring time accurately in spite of motion or variations in temperature, humidity, and air pressure."),
                        examples = listOf("The time traveler calibrated his chronometer before engaging the brass lever.")
                    )
                )
            ),
            DictionaryEntry(
                word = "dimension",
                phonetic = "/daɪˈmen.ʃən/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A measurable extent of some kind, such as length, breadth, depth, or time."),
                        examples = listOf("Time is the fourth dimension of spacetime.")
                    )
                )
            ),
            DictionaryEntry(
                word = "tactics",
                phonetic = "/ˈtæk.tɪks/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The art of disposing armed forces in order of battle and of organizing operations, or actions carefully planned to achieve a specific end."),
                        examples = listOf("Strategy without tactics is the slowest route to victory.")
                    )
                )
            ),
            DictionaryEntry(
                word = "sovereignty",
                phonetic = "/ˈsɒv.rɪn.ti/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("Supreme power or authority; the authority of a state to govern itself."),
                        examples = listOf("The sovereignty of the nation was preserved.")
                    )
                )
            ),
            DictionaryEntry(
                word = "aesthetic",
                phonetic = "/esˈθet.ɪk/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective / noun",
                        definitions = listOf("Concerned with beauty or the appreciation of beauty; a set of principles underlying work of an artist."),
                        examples = listOf("The book was crafted with meticulous aesthetic balance.")
                    )
                )
            ),
            DictionaryEntry(
                word = "ponder",
                phonetic = "/ˈpɒn.dər/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "verb",
                        definitions = listOf("Think about (something) carefully, especially before making a decision or reaching a conclusion."),
                        examples = listOf("He sat by the window to ponder the ancient riddle.")
                    )
                )
            ),
            DictionaryEntry(
                word = "vigilance",
                phonetic = "/ˈvɪdʒ.ɪ.ləns/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The action or state of keeping careful watch for possible danger or difficulties."),
                        examples = listOf("Eternal vigilance is the price of liberty.")
                    )
                )
            ),
            DictionaryEntry(
                word = "tenacious",
                phonetic = "/təˈneɪ.ʃəs/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Tending to keep a firm hold of something; clinging or adhering closely; persistent and determined."),
                        examples = listOf("Her tenacious pursuit of the truth uncovered the lost manuscript.")
                    )
                )
            ),
            DictionaryEntry(
                word = "metamorphosis",
                phonetic = "/ˌmet.əˈmɔː.fə.sɪs/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A change of the form or nature of a thing or person into a completely different one."),
                        examples = listOf("The caterpillar undergoes a complete metamorphosis into a butterfly.")
                    )
                )
            ),
            DictionaryEntry(
                word = "constancy",
                phonetic = "/ˈkɒn.stən.si/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The quality of being faithful, dependable, or unchanging over time."),
                        examples = listOf("His constancy in friendship was renowned.")
                    )
                )
            ),
            DictionaryEntry(
                word = "profound",
                phonetic = "/prəˈfaʊnd/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Very great or intense; having or showing great knowledge or insight."),
                        examples = listOf("A profound silence filled the room after his remarks.")
                    )
                )
            ),
            DictionaryEntry(
                word = "benevolent",
                phonetic = "/bəˈnev.əl.ənt/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "adjective",
                        definitions = listOf("Well meaning and kindly; serving a charitable rather than profit-making purpose."),
                        examples = listOf("A benevolent smile lit up his gentle face.")
                    )
                )
            ),
            DictionaryEntry(
                word = "harmony",
                phonetic = "/ˈhɑː.mə.ni/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("The combination of simultaneously sounded musical notes to produce chords; agreement or concord."),
                        examples = listOf("Living in complete harmony with nature.")
                    )
                )
            ),
            DictionaryEntry(
                word = "veracity",
                phonetic = "/vəˈræs.ə.ti/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("Conformity to facts; accuracy; habitual truthfulness."),
                        examples = listOf("Officials expressed doubt about the veracity of the claim.")
                    )
                )
            ),
            DictionaryEntry(
                word = "zeal",
                phonetic = "/ziːl/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("Great energy or enthusiasm in pursuit of a cause or an objective."),
                        examples = listOf("His Laura had worked with tireless zeal on the project.")
                    )
                )
            ),
            DictionaryEntry(
                word = "read",
                phonetic = "/riːd/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "verb",
                        definitions = listOf("Look at and comprehend the meaning of written or printed characters or symbols."),
                        examples = listOf("She spent the afternoon reading a novel.")
                    )
                )
            ),
            DictionaryEntry(
                word = "book",
                phonetic = "/bʊk/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A written or printed work consisting of pages bound together."),
                        examples = listOf("He opened the antique leather-bound book.")
                    )
                )
            ),
            DictionaryEntry(
                word = "page",
                phonetic = "/peɪdʒ/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("One side of a sheet of paper in a collection of sheets bound together, as in a book or magazine."),
                        examples = listOf("Turn to page forty-two.")
                    )
                )
            ),
            DictionaryEntry(
                word = "chapter",
                phonetic = "/ˈtʃæp.tər/",
                meanings = listOf(
                    DictionaryMeaning(
                        partOfSpeech = "noun",
                        definitions = listOf("A main division of a book, typically with a number or title."),
                        examples = listOf("The next chapter reveals the secret.")
                    )
                )
            )
        )
        list.associateBy { it.word.lowercase() }
    }
}
