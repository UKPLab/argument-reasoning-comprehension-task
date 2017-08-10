"""
Converts the original C-binary format of word2vec by Mikolov et al. to a txt format
"""
import sys


def __main__(path):
    from gensim.models import word2vec

    model = word2vec.Word2Vec.load_word2vec_format(path + '/GoogleNews-vectors-negative300.bin', binary=True)
    model.save_word2vec_format('/GoogleNews-vectors-negative300.txt', binary=False)


if __name__ == "__main__":
    __main__(sys.argv(1))
