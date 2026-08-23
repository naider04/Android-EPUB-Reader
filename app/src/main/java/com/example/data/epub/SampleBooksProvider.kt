package com.example.data.epub

import com.example.R
import com.example.data.model.EpubBook
import com.example.data.model.EpubChapter
import com.example.data.model.PageBlock

object SampleBooksProvider {

    fun getSampleBooks(): List<EpubBook> {
        return listOf(
            createArtOfWar(),
            createAliceInWonderland(),
            createTimeMachine()
        )
    }

    private fun createArtOfWar(): EpubBook {
        val chapters = listOf(
            EpubChapter(
                id = "art_war_ch1",
                title = "Chapter I: Laying Plans",
                rawHtml = "<h1>Chapter I: Laying Plans</h1><p>Sun Tzu said: The art of war is of vital importance to the State.</p>",
                blocks = listOf(
                    PageBlock.Title("Chapter I: Laying Plans", 1),
                    PageBlock.Paragraph("Sun Tzu said: The art of war is of vital importance to the State. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected."),
                    PageBlock.Paragraph("The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field."),
                    PageBlock.Paragraph("These are: The Moral Law; Heaven; Earth; The Commander; Method and discipline."),
                    PageBlock.Blockquote("The Moral Law causes the people to be in complete accord with their ruler, so that they will follow him regardless of their lives, undismayed by any danger."),
                    PageBlock.Paragraph("Heaven signifies night and day, cold and heat, times and seasons. Earth comprises distances, great and small; danger and security; open ground and narrow passes; the chances of life and death."),
                    PageBlock.Paragraph("The Commander stands for the virtues of wisdom, sincerely, benevolence, courage and strictness."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("All warfare is based on deception. Hence, when able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away; when far away, we must make him believe we are near."),
                    PageBlock.Paragraph("Hold out baits to entice the enemy. Feign disorder, and crush him. If he is secure at all points, be prepared for him. If he is in superior strength, evade him."),
                    PageBlock.Paragraph("If your opponent is of choleric temper, seek to irritate him. Pretend to be weak, that he may grow arrogant. If he is taking his ease, give him no rest. If his forces are united, separate them.")
                ),
                fullPlainText = "The art of war is of vital importance to the State..."
            ),
            EpubChapter(
                id = "art_war_ch2",
                title = "Chapter II: Waging War",
                rawHtml = "<h1>Chapter II: Waging War</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter II: Waging War", 1),
                    PageBlock.Paragraph("Sun Tzu said: In the operations of war, where there are in the field a thousand swift chariots, as many heavy chariots, and a hundred thousand mail-clad soldiers, with provisions enough to carry them a thousand li, the expenditure at home and at the front will reach the sum of a thousand ounces of silver per day."),
                    PageBlock.Paragraph("When you engage in actual fighting, if victory is long in coming, then men's weapons will grow dull and their ardor will be damped. If you lay siege to a town, you will exhaust your strength."),
                    PageBlock.Blockquote("Now, when your weapons are dulled, your ardor damped, your strength exhausted and your treasure spent, other chieftains will spring up to take advantage of your extremity. Then no man, however wise, will be able to avert the consequences that must ensue."),
                    PageBlock.Paragraph("Thus, though we have heard of stupid haste in war, cleverness has never been seen associated with long delays. There is no instance of a country having benefited from prolonged warfare."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("Bring war material with you from home, but forage on the enemy. Thus the army will have food enough for its needs. A skilled general does not raise a second levy, neither are his supply-wagons loaded more than twice.")
                ),
                fullPlainText = "In the operations of war..."
            ),
            EpubChapter(
                id = "art_war_ch3",
                title = "Chapter III: Attack by Stratagem",
                rawHtml = "<h1>Chapter III: Attack by Stratagem</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter III: Attack by Stratagem", 1),
                    PageBlock.Paragraph("Sun Tzu said: In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good. So, too, it is better to recapture an army entire than to destroy it."),
                    PageBlock.Blockquote("Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting."),
                    PageBlock.Paragraph("Thus the highest form of generalship is to balk the enemy's plans; the next best is to prevent the junction of the enemy's forces; the next in order is to attack the enemy's army in the field; and the worst policy of all is to besiege walled cities."),
                    PageBlock.Paragraph("Therefore the skillful leader subdues the enemy's troops without any fighting; he captures their cities without laying siege to them; he overthrows their kingdom without lengthy operations in the field."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("If you know the enemy and know yourself, you need not fear the result of a hundred battles. If you know yourself but not the enemy, for every victory gained you will also suffer a defeat. If you know neither the enemy nor yourself, you will succumb in every battle.")
                ),
                fullPlainText = "In the practical art of war..."
            ),
            EpubChapter(
                id = "art_war_ch4",
                title = "Chapter IV: Tactical Dispositions",
                rawHtml = "<h1>Chapter IV: Tactical Dispositions</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter IV: Tactical Dispositions", 1),
                    PageBlock.Paragraph("Sun Tzu said: The good fighters of old first put themselves beyond the possibility of defeat, and then waited for an opportunity of defeating the enemy."),
                    PageBlock.Paragraph("To secure ourselves against defeat lies in our own hands, but the opportunity of defeating the enemy is provided by the enemy himself."),
                    PageBlock.Paragraph("Thus the good fighter is able to secure himself against defeat, but cannot make certain of defeating the enemy."),
                    PageBlock.Blockquote("Hence the saying: One may KNOW how to conquer without being able to DO it."),
                    PageBlock.Paragraph("Security against defeat implies defensive tactics; ability to defeat the enemy means taking the offensive."),
                    PageBlock.Paragraph("Standing on the defensive indicates insufficient strength; attacking, a superabundance of strength.")
                ),
                fullPlainText = "The good fighters of old..."
            ),
            EpubChapter(
                id = "art_war_ch5",
                title = "Chapter V: Energy & Momentum",
                rawHtml = "<h1>Chapter V: Energy & Momentum</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter V: Energy & Momentum", 1),
                    PageBlock.Paragraph("Sun Tzu said: The control of a large force is the same principle as the control of a few men: it is merely a question of dividing up their numbers."),
                    PageBlock.Paragraph("Fighting with a large army under your command is nowise different from fighting with a small one: it is merely a question of instituting signs and signals."),
                    PageBlock.Paragraph("In all fighting, the direct method may be used for joining battle, but indirect methods will be needed in order to secure victory."),
                    PageBlock.Blockquote("Energy may be likened to the bending of a crossbow; decision, to the releasing of a trigger."),
                    PageBlock.Paragraph("Simulated disorder postulates perfect discipline; simulated fear postulates courage; simulated weakness postulates strength."),
                    PageBlock.Paragraph("Therefore the clever combatant looks to the effect of combined energy, and does not require too much from individuals.")
                ),
                fullPlainText = "The control of a large force..."
            )
        )

        return EpubBook(
            id = "book_art_of_war",
            title = "The Art of War",
            author = "Sun Tzu",
            description = "An ancient Chinese military treatise dating from the Late Spring and Autumn Period, composed of 13 chapters on strategy, tactics, and mindset.",
            coverResId = R.drawable.img_cover_art_war,
            chapters = chapters,
            isAsset = true
        )
    }

    private fun createAliceInWonderland(): EpubBook {
        val chapters = listOf(
            EpubChapter(
                id = "alice_ch1",
                title = "Chapter I: Down the Rabbit-Hole",
                rawHtml = "<h1>Chapter I: Down the Rabbit-Hole</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter I: Down the Rabbit-Hole", 1),
                    PageBlock.Paragraph("Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice 'without pictures or conversation?'"),
                    PageBlock.Paragraph("So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her."),
                    PageBlock.Blockquote("There was nothing so VERY remarkable in that; nor did Alice think it so VERY much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!'"),
                    PageBlock.Paragraph("In another moment down went Alice after it, never once considering in the world how in the world she was to get out again."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("The rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down, so suddenly that Alice had not a moment to think about stopping herself before she found herself falling down a very deep well."),
                    PageBlock.Paragraph("Either the well was very deep, or she fell very slowly, for she had plenty of time as she went down to look about her and to wonder what was going to happen next. First, she tried to look down and make out what she was coming to, but it was too dark to see anything; then she looked at the sides of the well, and noticed that they were filled with cupboards and book-shelves.")
                ),
                fullPlainText = "Alice was beginning to get very tired..."
            ),
            EpubChapter(
                id = "alice_ch2",
                title = "Chapter II: The Pool of Tears",
                rawHtml = "<h1>Chapter II: The Pool of Tears</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter II: The Pool of Tears", 1),
                    PageBlock.Paragraph("'Curiouser and curiouser!' cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); 'now I'm opening out like the largest telescope that ever was! Good-bye, feet!'"),
                    PageBlock.Paragraph("Just at this moment her head struck against the roof of the hall: in fact she was now more than nine feet high, and she at once took up the little golden key and hurried off to the garden door."),
                    PageBlock.Blockquote("Poor Alice! It was as much as she could do, lying down on one side, to look through into the garden with one eye; but to get through was more hopeless than ever: she sat down and began to cry again."),
                    PageBlock.Paragraph("She went on shedding gallons of tears, until there was a large pool all round her, about four inches deep and reaching half down the hall."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("After a time she heard a little pattering of feet in the distance, and she hastily dried her eyes to see what was coming. It was the White Rabbit returning, splendidly dressed, with a pair of white kid gloves in one hand and a large fan in the other.")
                ),
                fullPlainText = "Curiouser and curiouser cried Alice..."
            ),
            EpubChapter(
                id = "alice_ch3",
                title = "Chapter III: A Caucus-Race and a Long Tale",
                rawHtml = "<h1>Chapter III: A Caucus-Race</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter III: A Caucus-Race", 1),
                    PageBlock.Paragraph("They were indeed a queer-looking party that assembled on the bank—the birds with draggled feathers, the animals with their fur clinging close to them, and all dripping wet, cross, and uncomfortable."),
                    PageBlock.Paragraph("The first question of course was, how to get dry again: they had a consultation about this, and after a few minutes it seemed quite natural to Alice to find herself talking familiarly with them, as if she had known them all her life."),
                    PageBlock.Blockquote("'What I was going to say,' said the Dodo in an offended tone, 'was, that the best thing to get us dry would be a Caucus-race.'"),
                    PageBlock.Paragraph("'What IS a Caucus-race?' said Alice; not that she wanted much to know, but the Dodo had paused as if it thought that SOMEBODY ought to speak, and no one else seemed inclined to say anything."),
                    PageBlock.Paragraph("'Why,' said the Dodo, 'the best way to explain it is to do it.' First it marked out a race-course, in a sort of circle, and then all the party were placed along the course, here and there. There was no 'One, two, three, and away,' but they began running when they liked, and left off when they liked, so that it was not easy to know when the race was over.")
                ),
                fullPlainText = "They were indeed a queer-looking party..."
            ),
            EpubChapter(
                id = "alice_ch4",
                title = "Chapter IV: Advice from a Caterpillar",
                rawHtml = "<h1>Chapter IV: Advice from a Caterpillar</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter IV: Advice from a Caterpillar", 1),
                    PageBlock.Paragraph("The Caterpillar and Alice looked at each other for some time in silence: at last the Caterpillar took the hookah out of its mouth, and addressed her in a languid, sleepy voice."),
                    PageBlock.Blockquote("'Who are YOU?' said the Caterpillar. This was not an encouraging opening for a conversation."),
                    PageBlock.Paragraph("Alice replied, rather shyly, 'I—I hardly know, sir, just at present—at least I know who I WAS when I got up this morning, but I think I must have been changed several times since that.'"),
                    PageBlock.Paragraph("'What do you mean by that?' said the Caterpillar sternly. 'Explain yourself!'"),
                    PageBlock.Paragraph("'I can't explain MYSELF, I'm afraid, sir' said Alice, 'because I'm not myself, you see.' 'I don't see,' said the Caterpillar."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("'One side will make you grow taller, and the other side will make you grow shorter.' 'One side of WHAT? The other side of WHAT?' thought Alice to herself. 'Of the mushroom,' said the Caterpillar, just as if she had asked it aloud.")
                ),
                fullPlainText = "The Caterpillar and Alice looked at each other..."
            )
        )

        return EpubBook(
            id = "book_alice_wonderland",
            title = "Alice's Adventures in Wonderland",
            author = "Lewis Carroll",
            description = "The classic Victorian tale of a young girl named Alice who falls down a rabbit hole into a whimsical, subterranean fantasy world.",
            coverResId = R.drawable.img_cover_alice,
            chapters = chapters,
            isAsset = true
        )
    }

    private fun createTimeMachine(): EpubBook {
        val chapters = listOf(
            EpubChapter(
                id = "time_mach_ch1",
                title = "Chapter I: The Introduction",
                rawHtml = "<h1>Chapter I: The Introduction</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter I: The Introduction", 1),
                    PageBlock.Paragraph("The Time Traveller (for so it will be convenient to speak of him) was expounding a recondite matter to us. His grey eyes shone and twinkled, and his usually pale face was flushed and animated."),
                    PageBlock.Paragraph("The fire burnt brightly, and the soft radiance of the incandescent lights in the lilies of silver caught the bubbles that flashed and passed in our glasses."),
                    PageBlock.Blockquote("'You must follow me carefully. I shall have to controvert one or two ideas that are almost universally accepted. The geometry, for instance, they taught you at school is founded on a misconception.'"),
                    PageBlock.Paragraph("'Is not that rather a large thing to expect us to begin upon?' said Filby, an argumentative person with red hair."),
                    PageBlock.Paragraph("'I do not mean to ask you to accept anything without reasonable ground for it. You will soon admit as much as I need from you. You know of course that a mathematical line, a line of thickness nil, has no real existence. Nor has a mathematical plane. These things are mere abstractions.'"),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("'Nor, having only length, breadth, and thickness, can a cube have a real existence. There are really four dimensions, three which we call the three planes of Space, and a fourth, Time.'")
                ),
                fullPlainText = "The Time Traveller was expounding..."
            ),
            EpubChapter(
                id = "time_mach_ch2",
                title = "Chapter II: The Machine",
                rawHtml = "<h1>Chapter II: The Machine</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter II: The Machine", 1),
                    PageBlock.Paragraph("The thing the Time Traveller held in his hand was a glittering metallic framework, scarcely larger than a small clock, and very delicately made. There was ivory in it, and some transparent crystalline substance."),
                    PageBlock.Paragraph("And now I must be explicit, for this that follows—unless his explanation is to be accepted—is an absolutely unaccountable thing."),
                    PageBlock.Blockquote("He took one of the small octagonal tables that were scattered about the room, and set it in front of the fire, with two legs on the hearthrug. On this table he placed the mechanism."),
                    PageBlock.Paragraph("We all drew our chairs in toward the fire and watched him. The Time Traveller looked at us, and then put out his hand towards a lever. 'No,' he said suddenly. 'Lend me your hand.' And turning to a psychologist, he took that individual's hand in his own and told him to put out his forefinger and push the lever.")
                ),
                fullPlainText = "The thing the Time Traveller held in his hand..."
            ),
            EpubChapter(
                id = "time_mach_ch3",
                title = "Chapter III: In the Golden Age",
                rawHtml = "<h1>Chapter III: In the Golden Age</h1>",
                blocks = listOf(
                    PageBlock.Title("Chapter III: In the Golden Age", 1),
                    PageBlock.Paragraph("I stepped out upon the grass. The huge silver Sphinx looked down at me from above the rhododendron bushes. The white stone of which it was carved was faintly yellowed with age, and a reddish stain ran down from one eye."),
                    PageBlock.Paragraph("Looking round, with a sudden start I saw the figures in rich soft robes had stopped around me. They were coming toward me across the lawn."),
                    PageBlock.Blockquote("In another moment we were standing face to face, I and this fragile thing out of futurity. He came straight up to me and laughed into my eyes."),
                    PageBlock.Paragraph("He was a slight creature, perhaps four feet high, clad in a purple tunic, girdled at the waist with a leather belt. Sandals or buskins—I could not clearly see which—were on his feet; his legs were bare to the knees, and his head was bare."),
                    PageBlock.Divider(),
                    PageBlock.Paragraph("The delicacy of his structure struck me immediately. His flushed face reminded me of the more beautiful kind of consumptive—that hectic beauty of which we used to hear so much.")
                ),
                fullPlainText = "I stepped out upon the grass..."
            )
        )

        return EpubBook(
            id = "book_time_machine",
            title = "The Time Machine",
            author = "H.G. Wells",
            description = "A pioneering science fiction novella chronicling the journey of a Victorian English scientist who invents a time machine and travels to the year 802,701 AD.",
            coverResId = R.drawable.img_cover_time,
            chapters = chapters,
            isAsset = true
        )
    }
}
