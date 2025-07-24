package tictactoe.logic

import tictactoe.logic.GameLogic.Board
import tictactoe.logic.GameLogic.Cell
import tictactoe.logic.GameLogic.Mark

import org.scalatest.funsuite.AnyFunSuite

class BoardTest extends AnyFunSuite {

  test("Move on an empty cell should update the board") {
    val emptyBoard   = Board(13) // 13x13 board
    val updatedBoard = emptyBoard.move(Mark.X, 0, 0)

    assert(updatedBoard.isDefined)
    assert(updatedBoard.get.cells(0)(0).mark.contains(Mark.X))
  }

  test("Move on an occupied cell should return None") {
    val emptyBoard = Board(13)
    val boardWithMove = emptyBoard.move(Mark.X, 0, 0).get
    
    assert(boardWithMove.move(Mark.O, 0, 0).isEmpty)
  }

  test("Check if small board is full") {
    // Create a 3x3 board for easier testing of fullness
    val fullBoard = Board(
      Vector(
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.O)), Cell(Some(Mark.X))),
        Vector(Cell(Some(Mark.O)), Cell(Some(Mark.X)), Cell(Some(Mark.O))),
        Vector(Cell(Some(Mark.X)), Cell(Some(Mark.O)), Cell(Some(Mark.X)))
      )
    )

    assert(fullBoard.isFull)
  }

  test("Check if board is not full") {
    val emptyBoard = Board(13)
    assert(!emptyBoard.isFull)
    
    val partialBoard = emptyBoard.move(Mark.X, 0, 0).get
    assert(!partialBoard.isFull)
  }

  test("5-in-a-row horizontal win condition") {
    val emptyBoard = Board(13)
    // Place 5 X's in a row horizontally at position (0,0) to (4,0)
    val board = (0 until 5).foldLeft(emptyBoard) { (b, x) =>
      b.move(Mark.X, x, 0).get
    }

    assert(board.currentTurnWon)
  }

  test("5-in-a-row vertical win condition") {
    val emptyBoard = Board(13)
    // Place 5 X's in a column at position (0,0) to (0,4)
    val board = (0 until 5).foldLeft(emptyBoard) { (b, y) =>
      b.move(Mark.X, 0, y).get
    }

    assert(board.currentTurnWon)
  }

  test("5-in-a-row diagonal win condition") {
    val emptyBoard = Board(13)
    // Place 5 X's diagonally from (0,0) to (4,4)
    val board = (0 until 5).foldLeft(emptyBoard) { (b, i) =>
      b.move(Mark.X, i, i).get
    }

    assert(board.currentTurnWon)
  }

  test("5-in-a-row anti-diagonal win condition") {
    val emptyBoard = Board(13)
    // Place 5 X's anti-diagonally from (0,4) to (4,0)
    val board = (0 until 5).foldLeft(emptyBoard) { (b, i) =>
      b.move(Mark.X, i, 4 - i).get
    }

    assert(board.currentTurnWon)
  }

  test("4-in-a-row should not win") {
    val emptyBoard = Board(13)
    // Place only 4 X's in a row
    val board = (0 until 4).foldLeft(emptyBoard) { (b, x) =>
      b.move(Mark.X, x, 0).get
    }

    assert(!board.currentTurnWon)
  }

  test("5-in-a-row blocked on both ends should not win") {
    val emptyBoard = Board(13)
    // Place O at position 0, then 5 X's from 1-5, then O at position 6
    // Pattern: O X X X X X O
    val board = emptyBoard
      .move(Mark.O, 0, 0).get  // Block start
      .move(Mark.X, 1, 0).get  // X sequence starts
      .move(Mark.X, 2, 0).get
      .move(Mark.X, 3, 0).get
      .move(Mark.X, 4, 0).get
      .move(Mark.X, 5, 0).get  // X sequence ends
      .move(Mark.O, 6, 0).get  // Block end

    assert(!board.currentTurnWon)
  }

  test("5-in-a-row blocked on one end should win") {
    val emptyBoard = Board(13)
    // Place O at position 0, then 5 X's from 1-5, position 6 is empty
    // Pattern: O X X X X X _
    val board = emptyBoard
      .move(Mark.O, 0, 0).get  // Block start only
      .move(Mark.X, 1, 0).get  // X sequence
      .move(Mark.X, 2, 0).get
      .move(Mark.X, 3, 0).get
      .move(Mark.X, 4, 0).get
      .move(Mark.X, 5, 0).get

    assert(board.currentTurnWon)
  }

  test("5-in-a-row with no blocking should win") {
    val emptyBoard = Board(13)
    // Place 5 X's in the middle of board with space on both ends
    // Pattern: _ X X X X X _
    val board = (2 until 7).foldLeft(emptyBoard) { (b, x) =>
      b.move(Mark.X, x, 0).get
    }

    assert(board.currentTurnWon)
  }

  test("6-in-a-row blocked on both ends should not win") {
    val emptyBoard = Board(13)
    // Even with 6 in a row, if blocked on both ends it shouldn't win
    // Pattern: O X X X X X X O
    val board = emptyBoard
      .move(Mark.O, 0, 0).get  // Block start
      .move(Mark.X, 1, 0).get  // 6 X's
      .move(Mark.X, 2, 0).get
      .move(Mark.X, 3, 0).get
      .move(Mark.X, 4, 0).get
      .move(Mark.X, 5, 0).get
      .move(Mark.X, 6, 0).get
      .move(Mark.O, 7, 0).get  // Block end

    assert(!board.currentTurnWon)
  }

  test("Mixed pieces should not win") {
    val emptyBoard = Board(13)
    // Mix of X and O pieces
    val board = emptyBoard
      .move(Mark.X, 0, 0).get
      .move(Mark.O, 1, 0).get
      .move(Mark.X, 2, 0).get
      .move(Mark.O, 3, 0).get
      .move(Mark.X, 4, 0).get

    assert(!board.currentTurnWon)
  }
}
