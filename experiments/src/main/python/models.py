"""
Neural models
"""

import keras
import numpy as np
from keras.engine import Input
from keras.engine import Model
from keras.engine import merge
from keras.layers import Activation
from keras.layers import Convolution1D
from keras.layers import Flatten
from keras.layers import Lambda
from keras.layers import MaxPooling1D
from keras.utils.np_utils import accuracy
from theano.scalar import float32
from keras.preprocessing import sequence
from keras.models import Sequential
from keras.layers import Dense, Dropout, Embedding, LSTM, Bidirectional
from keras import backend

from attention_lstm import AttentionLSTM


def get_attention_lstm(word_index_to_embeddings_map, max_len, rich_context: bool=False, **kwargs):
    # converting embeddings to numpy 2d array: shape = (vocabulary_size, 300)
    embeddings = np.asarray([np.array(x, dtype=float32) for x in word_index_to_embeddings_map.values()])
    print('embeddings.shape', embeddings.shape)

    lstm_size = kwargs.get('lstm_size')
    dropout = kwargs.get('dropout')
    assert lstm_size
    assert dropout

    # define basic four input layers - for warrant0, warrant1, reason, claim
    sequence_layer_warrant0_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_warrant0_input")
    sequence_layer_warrant1_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_warrant1_input")
    sequence_layer_reason_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_reason_input")
    sequence_layer_claim_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_claim_input")
    sequence_layer_debate_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_debate_input")

    # now define embedded layers of the input
    embedded_layer_warrant0_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_warrant0_input)
    embedded_layer_warrant1_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_warrant1_input)
    embedded_layer_reason_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_reason_input)
    embedded_layer_claim_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_claim_input)
    embedded_layer_debate_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_debate_input)

    bidi_lstm_layer_reason = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM Reason')(embedded_layer_reason_input)
    bidi_lstm_layer_claim = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM Claim')(embedded_layer_claim_input)
    # add context to the attention layer
    bidi_lstm_layer_debate = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM Context')(embedded_layer_debate_input)

    if rich_context:
        # merge reason and claim
        context_concat = merge([bidi_lstm_layer_reason, bidi_lstm_layer_claim, bidi_lstm_layer_debate], mode='concat')
    else:
        context_concat = merge([bidi_lstm_layer_reason, bidi_lstm_layer_claim], mode='concat')

    # max-pooling
    max_pool_lambda_layer = Lambda(lambda x: keras.backend.max(x, axis=1, keepdims=False), output_shape=lambda x: (x[0], x[2]))
    max_pool_lambda_layer.supports_masking = True
    attention_vector = max_pool_lambda_layer(context_concat)

    attention_warrant0 = AttentionLSTM(lstm_size, attention_vector)(embedded_layer_warrant0_input)
    attention_warrant1 = AttentionLSTM(lstm_size, attention_vector)(embedded_layer_warrant1_input)

    # concatenate them
    dropout_layer = Dropout(dropout)(merge([attention_warrant0, attention_warrant1]))

    # and add one extra layer with ReLU
    dense1 = Dense(int(lstm_size / 2), activation='relu')(dropout_layer)
    output_layer = Dense(1, activation='sigmoid')(dense1)

    model = Model([sequence_layer_warrant0_input, sequence_layer_warrant1_input, sequence_layer_reason_input,
                   sequence_layer_claim_input, sequence_layer_debate_input], output=output_layer)
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

    from keras.utils.visualize_util import plot
    plot(model, show_shapes=True, to_file='/tmp/model-att.png')

    # from keras.utils.visualize_util import plot
    # plot(model, show_shapes=True, to_file='/tmp/attlstm.png')

    return model


def get_attention_lstm_intra_warrant(word_index_to_embeddings_map, max_len, rich_context: bool=False, **kwargs):
    # converting embeddings to numpy 2d array: shape = (vocabulary_size, 300)
    embeddings = np.asarray([np.array(x, dtype=float32) for x in word_index_to_embeddings_map.values()])
    print('embeddings.shape', embeddings.shape)

    lstm_size = kwargs.get('lstm_size')
    dropout = kwargs.get('dropout')
    assert lstm_size
    assert dropout

    # define basic four input layers - for warrant0, warrant1, reason, claim
    sequence_layer_warrant0_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_warrant0_input")
    sequence_layer_warrant1_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_warrant1_input")
    sequence_layer_reason_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_reason_input")
    sequence_layer_claim_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_claim_input")
    sequence_layer_debate_input = Input(shape=(max_len,), dtype='int32', name="sequence_layer_debate_input")

    # now define embedded layers of the input
    embedded_layer_warrant0_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_warrant0_input)
    embedded_layer_warrant1_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_warrant1_input)
    embedded_layer_reason_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_reason_input)
    embedded_layer_claim_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_claim_input)
    embedded_layer_debate_input = Embedding(embeddings.shape[0], embeddings.shape[1], input_length=max_len, weights=[embeddings], mask_zero=True)(sequence_layer_debate_input)

    bidi_lstm_layer_warrant0 = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM W0')(embedded_layer_warrant0_input)
    bidi_lstm_layer_warrant1 = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM W1')(embedded_layer_warrant1_input)
    bidi_lstm_layer_reason = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM Reason')(embedded_layer_reason_input)
    bidi_lstm_layer_claim = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM Claim')(embedded_layer_claim_input)
    # add context to the attention layer
    bidi_lstm_layer_debate = Bidirectional(LSTM(lstm_size, return_sequences=True), name='BiDiLSTM Context')(embedded_layer_debate_input)

    # max-pooling
    max_pool_lambda_layer = Lambda(lambda x: keras.backend.max(x, axis=1, keepdims=False), output_shape=lambda x: (x[0], x[2]))
    max_pool_lambda_layer.supports_masking = True
    # two attention vectors

    if rich_context:
        attention_vector_for_w0 = max_pool_lambda_layer(merge([bidi_lstm_layer_reason, bidi_lstm_layer_claim, bidi_lstm_layer_warrant1, bidi_lstm_layer_debate], mode='concat'))
        attention_vector_for_w1 = max_pool_lambda_layer(merge([bidi_lstm_layer_reason, bidi_lstm_layer_claim, bidi_lstm_layer_warrant0, bidi_lstm_layer_debate], mode='concat'))
    else:
        attention_vector_for_w0 = max_pool_lambda_layer(merge([bidi_lstm_layer_reason, bidi_lstm_layer_claim, bidi_lstm_layer_warrant1], mode='concat'))
        attention_vector_for_w1 = max_pool_lambda_layer(merge([bidi_lstm_layer_reason, bidi_lstm_layer_claim, bidi_lstm_layer_warrant0], mode='concat'))

    attention_warrant0 = AttentionLSTM(lstm_size, attention_vector_for_w0)(bidi_lstm_layer_warrant0)
    attention_warrant1 = AttentionLSTM(lstm_size, attention_vector_for_w1)(bidi_lstm_layer_warrant1)

    # concatenate them
    dropout_layer = Dropout(dropout)(merge([attention_warrant0, attention_warrant1]))

    # and add one extra layer with ReLU
    dense1 = Dense(int(lstm_size / 2), activation='relu')(dropout_layer)
    output_layer = Dense(1, activation='sigmoid')(dense1)

    model = Model([sequence_layer_warrant0_input, sequence_layer_warrant1_input, sequence_layer_reason_input,
                   sequence_layer_claim_input, sequence_layer_debate_input], output=output_layer)
    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

    from keras.utils.visualize_util import plot
    plot(model, show_shapes=True, to_file='/tmp/model-att.png')

    # from keras.utils.visualize_util import plot
    # plot(model, show_shapes=True, to_file='/tmp/attlstm.png')

    return model
